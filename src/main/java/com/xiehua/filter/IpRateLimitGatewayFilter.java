package com.xiehua.filter;

import com.google.common.collect.ImmutableSet;
import com.xiehua.authentication.IPFilter;
import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.redis.request.RedisSlidingWindowRequestRateLimiter;
import io.lettuce.core.api.StatefulRedisConnection;
import io.vavr.Tuple2;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;


@Slf4j
@Component
public class IpRateLimitGatewayFilter implements GatewayFilter, Ordered {

    public static final String GATEWAY_REQ_ID = "gateway:request:req_id_";

    public static final String GATEWAY_RULE = "gateway:rules:service_";

    public static final String GATEWAY_RULE_DEFAULT ="default:default";

    public static final String REQ_ID = "Request-ID";

    public static final String ROUTE_RULES = "route_rules";

    public static final Long EXP_TIME = 1000L;//过期时间

    private static final String SCHEME_REGEX = "[a-zA-Z]([a-zA-Z]|\\d|\\+|\\.|-)*:.*";

    static final Pattern schemePattern = Pattern.compile(SCHEME_REGEX);

    public ImmutableSet<RequestLimitRule> rules = ImmutableSet.of(RequestLimitRule.of(1, TimeUnit.MINUTES, 10000));

    //    @Autowired
//    @Qualifier("redisPool")
//    private GenericObjectPool<StatefulRedisConnection> pool;

    @Autowired
    private StatefulRedisConnection<String, String> connection;

    @Autowired
    private ReactiveRedisTemplate<String, String> template;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ip = IPFilter.getIpAddr(exchange);
        String serviceName = getServiceName(exchange);
        return writeReqInfo2Redis(exchange).zipWith(new RedisSlidingWindowRequestRateLimiter(connection, rules).overLimitWhenIncrementedReactive(ip)).publishOn(Schedulers.elastic()).flatMap(s -> {
            if (s.getT2()) {
                log.warn("请求数超过阈值ip:{}", ip);
                s.getT1().getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return s.getT1().getResponse().setComplete();
            } else {
                ServerWebExchange webExchange = s.getT1();
                webExchange.getAttributes().put(ROUTE_RULES, queryRouteInfo(exchange, serviceName));
                return chain.filter(webExchange);
            }
        });


        /**连接池的方式在高并发下有问题,现象:会导致http线程一直watting**/
//        try {
//            String ip = IPFilter.getIpAddr(exchange);
//            StatefulRedisConnection<String, String> connection = pool.borrowObject();
//            return  new RedisSlidingWindowRequestRateLimiter(connection, rules).overLimitWhenIncrementedReactive(ip).flatMap(s ->{
//                if (s) {
//                    log.warn("请求数超过阈值ip:{}", ip);
//                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
//                    return exchange.getResponse().setComplete();
//                } else {
//                    return chain.filter(exchange);
//                }
//            }).doFinally(t ->{
//                pool.returnObject(connection);
//            });
//        } catch (Exception e) {
//           log.error("IP限流器异常:{}",e);
//        }

    }


    @Override
    public int getOrder() {
        return -1000;
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
        String str = exchange.getRequest().getHeaders().getFirst(REQ_ID);
        //check req id
        if (StringUtils.isEmpty(str)) return writeRedis(exchange);
        return template.hasKey(GATEWAY_REQ_ID + str).flatMap(s -> {
            if (s) {
                return Mono.just(exchange);
            } else {
                return Mono.error(new RuntimeException("REQ_ID 不存在或已失效"));
            }
        });
    }

    /**
     * write redis
     **/
    private Mono<ServerWebExchange> writeRedis(ServerWebExchange exchange) {
        //put req id
        String reqId = UUID.randomUUID().toString().replace("-", "");
        ServerHttpRequest request = exchange.getRequest().mutate().header(REQ_ID, reqId).build();
        ServerWebExchange webExchange = exchange.mutate().request(request).response(exchange.getResponse()).build();
        //write req info to redis and set expire time for key 'req_id_xxx'
        String key = GATEWAY_REQ_ID + reqId;
        LocalDateTime now = LocalDateTime.now();
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
    private Map<String, String> queryRouteInfo(ServerWebExchange exchange, String serviceName) {
        //request info
        Map<String, String> map = readReq2Map(exchange);
        List list = Arrays.asList(ClientField.values()).stream().map(s -> {
            String field = s.getValue();
            String value = map.get(field);
            if (StringUtils.isEmpty(value)) return null;
            return field + ":" + value;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        if(list == null ||list.size() < 1) list.add(GATEWAY_RULE_DEFAULT);
        return (Map<String, String>) template.opsForHash().multiGet(GATEWAY_RULE + serviceName, list).map(s -> {
            List<String> strings = (List<String>) s;
            return strings.stream().map(m -> {
                String[] temp = m.split(":");
                Tuple2<String, String> t = new Tuple2(temp[0], temp[1]);
                return t;
            }).collect(Collectors.toMap(m -> m._1, n -> n._2, (x, y) -> y));

        }).switchIfEmpty(Mono.error(new RuntimeException(serviceName + "路由规则未配置(redis)"))).block();
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
