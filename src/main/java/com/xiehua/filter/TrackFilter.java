package com.xiehua.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.fun.Try;
import com.xiehua.track.Span;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import static com.xiehua.filter.RouteFilter.GATEWAY_ATTR_SERVER_NAME;
import static com.xiehua.filter.RouteFilter.HEAD_REQ_ID;
import static com.xiehua.filter.g.CounterFilter.GATEWAY_ATTR_REQ_TERM_ID;

/**
 * 调用链
 *
 * @Version V1.0
 */
@Component
@Slf4j
public class TrackFilter implements GatewayFilter, XiehuaOrdered {

    public static final String REDIS_GATEWAY_TRACK = "gateway:track:req_";//redis track

    public static final Long EXP_SECONDS = 60 * 5L;//过期时间(5分钟)

    @Autowired
    private StatefulRedisConnection<String, String> connection;

    @Autowired
    private ObjectMapper mapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return Mono.justOrEmpty(exchange).flatMap(Try.of(s -> {
            String traceId = exchange.getRequest().getHeaders().getFirst(HEAD_REQ_ID);
            if (StringUtils.isEmpty(traceId)) return chain.filter(exchange);
            String spanId = exchange.getAttribute(GATEWAY_ATTR_REQ_TERM_ID);
            if (StringUtils.isEmpty(spanId)) return chain.filter(exchange);
            String serverName = exchange.getAttribute(GATEWAY_ATTR_SERVER_NAME);
            if (StringUtils.isEmpty(serverName)) return chain.filter(exchange);
            String key = REDIS_GATEWAY_TRACK + traceId;

            String url = exchange.getRequest().getURI().toString();

            Span currentSpan = new Span(traceId, spanId, serverName, url, LocalDateTime.now(), new ArrayList<>());
            return asyncRW(key, currentSpan, exchange, chain);
        }));

    }

    private Mono<Void> syncRW(String key, Span currentSpan, ServerWebExchange exchange, GatewayFilterChain chain) throws IOException {
        RedisCommands<String, String> commands = syncCommands();
        if (commands.exists(key) == 0) {//不存在
            commands.set(key, mapper.writeValueAsString(currentSpan));
            commands.expire(key,EXP_SECONDS);
            return chain.filter(exchange);
        }
        //存在
        Span span = mapper.readValue(commands.get(key), new TypeReference<Span>() {
        });
        span.addChild(currentSpan);
        commands.set(key, mapper.writeValueAsString(span));
        commands.expire(key,EXP_SECONDS);
        return chain.filter(exchange);
    }

    private Mono<Void> asyncRW(String key, Span currentSpan, ServerWebExchange exchange, GatewayFilterChain chain) throws InterruptedException, ExecutionException, IOException {
        RedisAsyncCommands<String, String> commands = asyncCommands();
        if (commands.exists(key).get() == 0) {//不存在
            commands.set(key, mapper.writeValueAsString(currentSpan));
            commands.expire(key,EXP_SECONDS);
            return chain.filter(exchange);
        }
        //存在
        Span span = mapper.readValue(commands.get(key).get(), new TypeReference<Span>() { });
        span.addChild(currentSpan);
        commands.set(key, mapper.writeValueAsString(span));
        commands.expire(key,EXP_SECONDS);
        return chain.filter(exchange);
    }

    private RedisCommands<String, String> syncCommands() {
        return connection.sync();
    }

    private RedisAsyncCommands<String, String> asyncCommands() {
        return connection.async();
    }

    @Override
    public int getOrder() {
        return TRACK_ORDER;
    }
}