package com.xiehua.filter;

import com.google.common.collect.ImmutableSet;
import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.redis.request.RedisSlidingWindowRequestRateLimiter;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

import static com.xiehua.component.GateWayComponent.HEAD_REQ_ID;


@Slf4j
@Component
public class RateLimitIpFilter implements GatewayFilter, XiehuaOrdered {

    public static final String REDIS_GATEWAY_RATELIMIT = "gateway:ratelimit:req_";//redis track

    public static final Long EXP_SECONDS = 60 * 3L;//过期时间（3分钟）

    public ImmutableSet<RequestLimitRule> rules = ImmutableSet.of(RequestLimitRule.of(1, TimeUnit.SECONDS, 1000));

    //    @Autowired
//    @Qualifier("redisPool")
//    private GenericObjectPool<StatefulRedisConnection> pool;

    @Autowired
    private StatefulRedisConnection<String, String> connection;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String key = REDIS_GATEWAY_RATELIMIT + exchange.getRequest().getHeaders().getFirst(HEAD_REQ_ID);
        RedisCommands<String, String> commands = connection.sync();
        if (commands.exists(key) == 1) return chain.filter(exchange);
        commands.set(key, EXP_SECONDS + "");
        commands.expire(key, EXP_SECONDS);

        String ip = JwtAuthenticationFilter.getIpAddr(exchange);
        if (new RedisSlidingWindowRequestRateLimiter(connection, rules).overLimitWhenIncremented(ip)) {
            log.warn("请求数超过阈值ip:{}", ip);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);


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
        return RATE_LIMIT_IP_ORDER;
    }

}
