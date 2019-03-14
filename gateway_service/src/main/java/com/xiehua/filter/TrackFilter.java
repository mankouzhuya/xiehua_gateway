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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.xiehua.filter.RouteFilter.*;

/**
 * 调用链
 *
 * @Version V1.0
 */
//@Component
//@Slf4j
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
            String serverName = exchange.getAttribute(GATEWAY_ATTR_SERVER_NAME);
            if (StringUtils.isEmpty(serverName)) return chain.filter(exchange);
            String traceId = exchange.getRequest().getHeaders().getFirst(HEAD_REQ_ID);
            if (StringUtils.isEmpty(traceId)) return chain.filter(exchange);
            String spanId = exchange.getRequest().getHeaders().getFirst(HEAD_FROM_ID);

            String key = REDIS_GATEWAY_TRACK + traceId;
            String url = exchange.getRequest().getURI().toString();

            if (StringUtils.isEmpty(spanId)) spanId = exchange.getRequest().getHeaders().getFirst(HEAD_ITERM_ID);

            Span currentSpan = new Span(traceId, spanId, url, LocalDateTime.now(),null, new ArrayList<>(),null);

            return asyncRW(key,currentSpan,exchange,chain);

        }));

    }


    private Span findParentSpan(Span span,String spanId){
        if(spanId.equals(span.getSpanId())) return span;
        List<Span> childs = span.getChilds();
        if(childs != null && childs.size() > 1){
            for(int i = 0;i< childs.size(); i++){
                Span temp = childs.get(i);
                if(spanId.equals(temp.getSpanId())) return temp;
                findParentSpan(temp,spanId);
            }
        }
        return null;
    }


    private Mono<Void> asyncRW(String key, Span currentSpan, ServerWebExchange exchange, GatewayFilterChain chain) throws InterruptedException, ExecutionException, IOException {
        RedisAsyncCommands<String, String> commands = asyncCommands();
        if (commands.exists(key).get() == 0) {//不存在
            commands.set(key, mapper.writeValueAsString(currentSpan));
            commands.expire(key,EXP_SECONDS);
            return chain.filter(exchange);
        }
        //存在
        Span rootSpan = mapper.readValue(commands.get(key).get(), new TypeReference<Span>() { });
        Span parentSpan = findParentSpan(rootSpan,currentSpan.getSpanId());
        if(parentSpan == null) return chain.filter(exchange);
        parentSpan.addChild(currentSpan);

        commands.set(key, mapper.writeValueAsString(rootSpan));
        commands.expire(key,EXP_SECONDS);
        return chain.filter(exchange);
    }


    private RedisCommands<String, String> syncCommands() {
        return connection.sync();
    }

    private RedisAsyncCommands<String, String> asyncCommands() {
        return connection.async();
    }


    private Mono<Void> syncRW(String key, Span currentSpan, ServerWebExchange exchange, GatewayFilterChain chain) throws IOException {
        RedisCommands<String, String> commands = syncCommands();
        if (commands.exists(key) == 0) {//不存在
            commands.set(key, mapper.writeValueAsString(currentSpan));
            commands.expire(key,EXP_SECONDS);
            return chain.filter(exchange);
        }
        //存在
        Span rootSpan = mapper.readValue(commands.get(key), new TypeReference<Span>() { });
        Span parentSpan = findParentSpan(rootSpan,currentSpan.getSpanId());
        if(parentSpan == null) return chain.filter(exchange);
        parentSpan.addChild(currentSpan);

        commands.set(key, mapper.writeValueAsString(rootSpan));
        commands.expire(key,EXP_SECONDS);
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return TRACK_ORDER;
    }
}