package com.xiehua.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.cache.SimpleCache;
import com.xiehua.cache.dto.SimpleKvDTO;
import com.xiehua.config.dto.CustomConfig;
import com.xiehua.exception.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.vavr.Tuple2;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.xiehua.converter.ServerHttpBearerAuthenticationConverter.GATEWAY_ATTR_JWT;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;


@Slf4j
@Component
public class RouteFilter implements GatewayFilter, XiehuaOrdered {

    public static final String REDIS_GATEWAY_REQUEST_ID_PREFIX = "gateway:request:req_id_";//redis global request info

    public static final String REDIS_GATEWAY_SERVICE_RULE = "gateway:rules:service_";//redis service route rules

    public static final String REDIS_GATEWAY_RULE_DEFAULT = "default:default";//default route rule

    public static final String REDIS_GATEWAY_LOGIN_PREFIX = "gateway:login:tid_";//redis login info( jwt claims),the key is claims.id

    public static final String REDIS_GATEWAY_ONLINE_PREFIX = "gateway:online:account_";//redis login info( jwt claims),the key is user account

    public static final String GATEWAY_ATTR_ROUTE_RULES = "gateway_attr_route_rules";// gateway attr route rules

    public static final String GATEWAY_ATTR_SERVER_NAME = "gateway_attr_server_name";//服务名字

    public static final String HEAD_REQ_ID = "Request-ID";//global request id,write to request head

    public static final String HEAD_ITERM_ID = "Requst-Iterm-ID";//每个单独请求分配一共req id

    public static final String HEAD_FROM_ID = "Requst-From-ID";//每个单独请求分配一共req id

    public static final String HEAD_REQ_JTI = "JTI";//claims.id,write to request head

    public static final Long EXP_TIME = 60 * 1L;//过期时间（1分钟）

    private static final String SCHEME_REGEX = "[a-zA-Z]([a-zA-Z]|\\d|\\+|\\.|-)*:.*";

    static final Pattern schemePattern = Pattern.compile(SCHEME_REGEX);

    @Autowired
    private ReactiveRedisTemplate<String, String> template;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SimpleCache defaultCache;


    @Autowired
    private CustomConfig customConfig;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return Mono.just(exchange)
                .flatMap(s -> writeReqInfo2Redis(s))
                .publishOn(Schedulers.elastic())
                .map(t -> {
                    String serviceName = getServiceName(exchange);
                    t.getAttributes().put(GATEWAY_ATTR_SERVER_NAME, serviceName);
                    t.getAttributes().put(GATEWAY_ATTR_ROUTE_RULES, queryRouteInfo(t, serviceName));
                    return t;
                }).flatMap(m -> chain.filter(m));
    }


    @Override
    public int getOrder() {
        return ROUTE_PRE_ORDER;
    }

    /**
     * get service name
     **/
    private String getServiceName(ServerWebExchange exchange) {
        Route route = exchange.getRequiredAttribute(GATEWAY_ROUTE_ATTR);
        URI uri = exchange.getRequest().getURI();
        boolean encoded = containsEncodedParts(uri);
        URI routeUri = route.getUri();
        if (hasAnotherScheme(routeUri)) {
            // this is a special url, save scheme to special attribute
            // replace routeUri with schemeSpecificPart
            exchange.getAttributes().put(GATEWAY_SCHEME_PREFIX_ATTR, routeUri.getScheme());
            routeUri = URI.create(routeUri.getSchemeSpecificPart());
        }

        URI requestUrl = UriComponentsBuilder.fromUri(uri)
                .uri(routeUri)
                .build(encoded)
                .toUri();
        return requestUrl.getHost();
    }


    /**
     * override request head and put head into redis
     **/
    private Mono<ServerWebExchange> writeReqInfo2Redis(ServerWebExchange exchange) {
        String key = REDIS_GATEWAY_REQUEST_ID_PREFIX + exchange.getRequest().getHeaders().getFirst(HEAD_REQ_ID);
        return template.hasKey(key).flatMap(s -> {
            if (s) {
                return Mono.just(exchange);
            } else {
                return writeRedis(exchange,key);
            }
        });
    }

    /**
     * write redis
     **/
    private Mono<ServerWebExchange> writeRedis(ServerWebExchange exchange,String key) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        //check jwt attr
        Claims claims = exchange.getAttribute(GATEWAY_ATTR_JWT);
        LocalDateTime now = LocalDateTime.now();
        if (claims != null) {
            builder.header(HEAD_REQ_JTI, claims.getId());
            LocalDateTime exp = claims.getExpiration().toInstant().atZone(ZoneOffset.systemDefault()).toLocalDateTime();
            //访问退出登录接口
            if (exchange.getRequest().getURI().getRawPath().contains("logout")) {
                exp = LocalDateTime.now().plusMinutes(1);
            }
            //write jwt to redis
            template.opsForHash()
                    .putAll(REDIS_GATEWAY_LOGIN_PREFIX + claims.getId(), claims.entrySet().stream().map(s -> new Tuple2(s.getKey(), s.getValue().toString())).collect(Collectors.toMap(k -> k._1, v -> v._2, (x, y) -> y)))
                    .then(template.expire(REDIS_GATEWAY_LOGIN_PREFIX + claims.getId(), Duration.between(now, exp)))
                    .then(template.opsForValue().set(REDIS_GATEWAY_ONLINE_PREFIX + claims.getAudience(), claims.getId()))
                    .then(template.expire(REDIS_GATEWAY_ONLINE_PREFIX + claims.getAudience(), Duration.between(now, exp)))
                    .subscribe();
            //update response
            if (!exchange.getRequest().getURI().getRawPath().contains("logout")) {
                claims.setExpiration(Date.from(LocalDateTime.now().plusSeconds(customConfig.getJwtExpiration()).atZone(ZoneId.systemDefault()).toInstant()));
                exchange.getResponse().getHeaders().put(HttpHeaders.AUTHORIZATION, Arrays.asList(Jwts.builder().setClaims(claims).signWith(customConfig.getJwtSingKey()).compact()));
            }
        }

        //build req
        ServerHttpRequest request = builder.build();
        ServerWebExchange webExchange = exchange.mutate().request(request).response(exchange.getResponse()).build();
        //write req info to redis and set expire time for key 'req_id_xxx'
        LocalDateTime end = now.plusSeconds(EXP_TIME);
        return template.opsForHash().putAll(key, readReq2Map(webExchange)).then(template.expire(key, Duration.between(now, end))).then(Mono.just(webExchange));
    }

    /**
     * read web exchange to map
     **/
    private Map<String, String> readReq2Map(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().entrySet().stream().map(s -> {
            Tuple2<String, String> t = new Tuple2(s.getKey(), s.getValue().get(0));
            return t;
        }).collect(Collectors.toMap(s -> s._1, t -> t._2, (x, y) -> y));
    }

    /**
     * query route from redis
     **/
    private Map<String, String> queryRouteInfo(ServerWebExchange exchange, String serviceName){
        //request info
        Map<String, String> map = readReq2Map(exchange);
        List<String> list = Arrays.asList(ClientField.values()).stream().map(s -> {
            String field = s.getValue();
            String value = map.get(field);
            if (StringUtils.isEmpty(value)) return null;
            return field + ":" + value;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        if (list == null || list.size() < 1) list.add(REDIS_GATEWAY_RULE_DEFAULT);
        //load rules
        String serviceKey = REDIS_GATEWAY_SERVICE_RULE + serviceName;
        Map<String, String> ruleMap = loadLocalCache(serviceKey)
                .stream()
                .collect(Collectors.toMap(m -> m.getKey(), n -> n.getValue(), (x, y) -> y));
        Map<String, String> fmap = list
                .stream()
                .filter(s -> ruleMap.containsKey(s))
                .map(v -> {
                    String val = ruleMap.get(v);
                    String[] temp = val.split(":");
                    Tuple2<String, String> t = new Tuple2(temp[0], temp[1]);
                    return t;
                }).collect(Collectors.toMap(m -> m._1, n -> n._2, (x, y) -> y));
        if(CollectionUtils.isEmpty(fmap)) throw new BizException(serviceKey + "路由规则未配置(redis):"+list.toString());
        return fmap;
    }


    //load config form local cache
    private List<SimpleKvDTO> loadLocalCache(String service)  {
        //query local cache
        String key = defaultCache.genKey(service);
        String value = defaultCache.get(key);
        if(!StringUtils.isBlank(value)) {
            try {
                return mapper.readValue(value,new TypeReference<List<SimpleKvDTO>>() {});
            } catch (IOException e) {
                log.error("反序列化失败:{}",e);
            }
        }
        //query redis
        ReactiveHashOperations<String,String,String> opsForHash = template.opsForHash();
        List<SimpleKvDTO> rules = opsForHash.entries(service).map(s->{
            SimpleKvDTO dto = new SimpleKvDTO();
            dto.setKey(s.getKey());
            dto.setValue(s.getValue());
            return dto;
        }).collectList().block();
        if(CollectionUtils.isEmpty(rules)) throw new RuntimeException(service + "路由规则未配置(redis)");
        try {
            defaultCache.put(key,mapper.writeValueAsString(rules));
        } catch (JsonProcessingException e) {
            log.error("序列化失败:{}",e);
        }
        return rules;
    }


    private boolean hasAnotherScheme(URI uri) {
        return schemePattern.matcher(uri.getSchemeSpecificPart()).matches() && uri.getHost() == null && uri.getRawPath() == null;
    }


    @AllArgsConstructor
    public enum ClientField {

        version("版本", "version");

        private String name;

        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

}
