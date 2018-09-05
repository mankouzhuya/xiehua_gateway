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

import com.xiehua.config.dto.CustomConfig;
import com.xiehua.config.dto.jwt.JwtUser;
import com.xiehua.exception.BizException;
import com.xiehua.fun.Try;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.Key;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This converter extracts a bearer token from a WebExchange and
 * returns an Authentication object if the JWT token is valid.
 * Validity means is well formed and signature is correct
 */
@Component
public class ServerHttpBearerAuthenticationConverter implements Function<ServerWebExchange, Mono<UsernamePasswordAuthenticationToken>> {

    private static final Logger logger = LoggerFactory.getLogger(ServerHttpBearerAuthenticationConverter.class);

    public static final String BEARER = "Bearer ";

    public static final String REDIS_PREFIX = "gateway:user:power_";


    @Autowired
    private ReactiveRedisOperations<String, String> operations;

    @Autowired
    private CustomConfig customConfig;

    /**
     * Apply this function to the current WebExchange, an Authentication object
     * is returned when completed.
     *
     * @param serverWebExchange
     * @return
     */
    @Override
    public Mono<UsernamePasswordAuthenticationToken> apply(ServerWebExchange serverWebExchange) {
        serverWebExchange.getRequest().getPath().contextPath();
        String path = serverWebExchange.getRequest().getURI().getPath();
        if(StringUtils.isEmpty(path)) throw new BizException("访问路径不合法:{}",path);
        String[] serverIds = path.split("/");
        if(serverIds == null || serverIds.length < 1) throw new BizException("访问的 service id 不存在");
        return Mono.justOrEmpty(serverWebExchange)
                .map(s -> {
                    String u = s.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                    if(StringUtils.isEmpty(u)) {
                        HttpCookie cookie = s.getRequest().getCookies().getFirst(HttpHeaders.AUTHORIZATION);
                        if(Objects.isNull(cookie)) throw new BadCredentialsException("没有token或token已失效");
                        u = cookie.getValue();
                        if(u.contains("%")) u = new String(Base64.getDecoder().decode(u));
                        if(!u.startsWith(BEARER)) u = BEARER + u;
                    };
                    if(Objects.isNull(u)) throw new BadCredentialsException("没有token或token已失效");
                    return u;
                })
                .filter(Objects::nonNull)
                .filter(t -> t.length() > BEARER.length())
                .map(m -> m.substring(BEARER.length(), m.length()))
                .filter(n -> !n.isEmpty())
                .map(i -> Jwts.parser().setSigningKey(customConfig.getJwtSingKey()).parseClaimsJws(i).getBody())
                .flatMap(u ->{
                    if(StringUtils.isEmpty(u.getSubject())) throw new BadCredentialsException("没有不合法");
                    return Mono.just(u).zipWith(Mono.fromCallable(() -> operater().get(genKey(u.getSubject()),serverIds[1])));
                }).publishOn(Schedulers.elastic())
                .map(r -> buildAuthenticationToken(r))
                .filter(Objects::nonNull);
    }

    private UsernamePasswordAuthenticationToken buildAuthenticationToken(Tuple2<Claims,Mono<String>> r){
        List<GrantedAuthority> list = r.getT2().switchIfEmpty(Mono.error(new BadCredentialsException("你没有权限访问该系统"))).flatMapIterable(s -> Arrays.asList(s.split(","))).map(SimpleGrantedAuthority::new).toStream().collect(Collectors.toList());
        UserDetails userDetails = new JwtUser(r.getT1().getSubject(),r.getT1().getAudience(),r.getT1().getId(),list);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, r.getT1(), userDetails.getAuthorities());
        return authentication;
    }

    private String genKey(String gid){
        return REDIS_PREFIX + gid;
    }

    private ReactiveHashOperations<String, String, String> operater(){
        ReactiveHashOperations<String, String, String> hashOperations =  operations.opsForHash();
        return hashOperations;
    }

}