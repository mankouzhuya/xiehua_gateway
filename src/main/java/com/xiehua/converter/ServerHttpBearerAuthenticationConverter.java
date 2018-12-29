/*
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xiehua.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.cache.SimpleCache;
import com.xiehua.cache.dto.SimpleKvDTO;
import com.xiehua.component.SpringComponent;
import com.xiehua.config.dto.CustomConfig;
import com.xiehua.config.dto.jwt.JwtUser;
import com.xiehua.exception.BizException;
import com.xiehua.filter.RouteFilter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.xiehua.filter.RouteFilter.GATEWAY_ATTR_SERVER_NAME;

/**
 * This converter extracts a bearer token from a WebExchange and
 * returns an Authentication object if the JWT token is valid.
 * Validity means is well formed and signature is correct
 */
@Component
public class ServerHttpBearerAuthenticationConverter implements Function<ServerWebExchange, Mono<UsernamePasswordAuthenticationToken>> {

    private static final Logger logger = LoggerFactory.getLogger(ServerHttpBearerAuthenticationConverter.class);

    public static final String BEARER = "Bearer ";//jwt 头

    public static final String GATEWAY_ATTR_JWT = "gateway_attr_jwt";//geteway attr jwt claims

    public static final String REDIS_GATEWAY_USER_POWER_PREFIX = "gateway:user:power_";//redis user power

    public static final String SYS_GATEWAY = "gateway";//gateway提供的web管理页面,这是需要特殊处理的服务名字(gateway服务为保留服务名,访问gateway服务是需要特殊处理)

    @Autowired
    private ReactiveRedisOperations<String, String> operations;

    @Autowired
    private SimpleCache defaultCache;

    @Autowired
    private CustomConfig customConfig;

    @Autowired
    private SpringComponent component;

    @Autowired
    private RouteFilter filter;

    @Autowired
    private ObjectMapper mapper;

    /**
     * Apply this function to the current WebExchange, an Authentication object
     * is returned when completed.
     *
     * @param serverWebExchange
     * @return
     */
    @Override
    public Mono<UsernamePasswordAuthenticationToken> apply(ServerWebExchange serverWebExchange) {
        return Mono.justOrEmpty(serverWebExchange)
                .map(s -> {
                    //put server_id
                    String path = serverWebExchange.getRequest().getURI().getPath();
                    if (StringUtils.isEmpty(path)) throw new BizException("访问路径不合法:{}", path);
                    String[] serverIds = path.split("/");
                    if (serverIds == null || serverIds.length < 1) throw new BizException("访问的 service id 不存在");
                    serverWebExchange.getAttributes().put(GATEWAY_ATTR_SERVER_NAME, serverIds[1]);
                    //get token
                    String u = s.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                    if (SYS_GATEWAY.equals(serverIds[1])) {//check access gateway web service
                        HttpCookie cookie = s.getRequest().getCookies().getFirst(HttpHeaders.AUTHORIZATION);
                        if (Objects.isNull(cookie)) throw new BadCredentialsException("没有token或token已失效");
                        u = cookie.getValue();
                        if (u.contains("%")) u = new String(Base64.getDecoder().decode(u));
                        if (!u.startsWith(BEARER)) u = BEARER + u;
                    }
                    if (Objects.isNull(u)) throw new BadCredentialsException("没有token或token已失效");
                    return u;
                })
                //validate token
                .filter(Objects::nonNull)
                .filter(t -> t.length() > BEARER.length())
                .map(m -> m.substring(BEARER.length(), m.length()))
                .filter(n -> !n.isEmpty())
                //parser token
                .map(i -> Jwts.parser().setSigningKey(customConfig.getJwtSingKey()).parseClaimsJws(i).getBody())
                .publishOn(Schedulers.elastic())
                .flatMap(e -> {
                    if (Objects.isNull(e.getSubject())) throw new BadCredentialsException("token不合法");
                    if (serverWebExchange.getAttributes().get(GATEWAY_ATTR_SERVER_NAME).equals(SYS_GATEWAY)) {
                        return Mono.just(e).zipWith(Mono.fromCallable(() -> loadLocalCache(e.getSubject(),SYS_GATEWAY)));
                    } else {
                        return Mono.just(e)
                                .zipWith(component.getBean(RouteLocator.class).getRoutes()
                                        .filter(w -> w.getFilters().contains(filter))
                                        .filterWhen(k -> k.getPredicate().apply(serverWebExchange))
                                        .next()
                                        //TODO: error handling
                                        .map(o -> {
                                            if (logger.isDebugEnabled()) logger.debug("Route matched: " + o.getId());
                                            validateRoute(o, serverWebExchange);
                                            return o;
                                        })
                                        .map(v -> loadLocalCache(e.getSubject(),v.getUri().getHost())));

                    }
                })
                .map(r -> buildAuthenticationToken(r, serverWebExchange))
                .filter(Objects::nonNull);
    }

    private UsernamePasswordAuthenticationToken buildAuthenticationToken(Tuple2<Claims, Mono<String>> r, ServerWebExchange serverWebExchange) {
        List<GrantedAuthority> list = r.getT2().switchIfEmpty(Mono.error(new BadCredentialsException("你没有权限访问该系统"))).flatMapIterable(s -> Arrays.asList(s.split(","))).map(SimpleGrantedAuthority::new).toStream().collect(Collectors.toList());
        UserDetails userDetails = new JwtUser(r.getT1().getSubject(), r.getT1().getAudience(), r.getT1().getId(), r.getT1().getIssuer(), list);
        serverWebExchange.getAttributes().put(GATEWAY_ATTR_JWT, r.getT1());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, r.getT1(), userDetails.getAuthorities());
        return authentication;
    }

    private String genKey(String gid) {
        return REDIS_GATEWAY_USER_POWER_PREFIX + gid;
    }

    private ReactiveHashOperations<String, String, String> operater() {
        ReactiveHashOperations<String, String, String> hashOperations = operations.opsForHash();
        return hashOperations;
    }

    //load config form local cache
    private Mono<String> loadLocalCache(String subject, String host){
        String k = genKey(subject);
        String key = defaultCache.genKey(k);
        String value = defaultCache.get(key);
        List<String> permissions;
        if(!StringUtils.isEmpty(value)) {
            try {
                List<SimpleKvDTO> list = mapper.readValue(value,new TypeReference<List<SimpleKvDTO>>() {});
                permissions = list.stream().filter(s -> s.getKey().equals(host)).map(m -> m.getValue()).collect(Collectors.toList());
                if(CollectionUtils.isEmpty(permissions)) throw new BadCredentialsException("你没有权限访问该系统");
                return Mono.just(permissions.get(0));
            } catch (IOException e) {
                logger.error("反序列化失败:{}",e);
            }
        }
        //query redis
        List<SimpleKvDTO> userPermissions = operater().entries(k).map(s->{
            SimpleKvDTO dto = new SimpleKvDTO();
            dto.setKey(s.getKey());
            dto.setValue(s.getValue());
            return dto;
        }).collectList().block();
        if(CollectionUtils.isEmpty(userPermissions)) throw new BadCredentialsException("你没有权限访问该系统");
        try {
            defaultCache.put(key,mapper.writeValueAsString(userPermissions));
        } catch (JsonProcessingException e) {
            logger.error("序列化失败:{}",e);
        }
        permissions = userPermissions.stream().filter(s ->s.getKey().equals(host)).map(m -> m.getValue()).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(permissions)) throw new BadCredentialsException("你没有权限访问该系统");
        return Mono.just(permissions.get(0));
    }

    /**
     * Validate the given handler against the current request.
     * <p>The default implementation is empty. Can be overridden in subclasses,
     * for example to enforce specific preconditions expressed in URL mappings.
     *
     * @param route    the Route object to validate
     * @param exchange current exchange
     * @throws Exception if validation failed
     */
    @SuppressWarnings("UnusedParameters")
    protected void validateRoute(Route route, ServerWebExchange exchange) {
    }

}