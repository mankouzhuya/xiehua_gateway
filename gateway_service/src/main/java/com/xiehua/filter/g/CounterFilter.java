package com.xiehua.filter.g;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.cache.SimpleCache;
import com.xiehua.filter.XiehuaOrdered;
import com.xiehua.fun.Try;
import com.xiehua.support.wrap.dto.ReqDTO;
import com.xiehua.track.Span;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.xiehua.filter.RouteFilter.HEAD_ITERM_ID;
import static com.xiehua.filter.TrackFilter.EXP_SECONDS;
import static com.xiehua.support.wrap.collect.CountTool.ATTR_REQ_ITEM;

/**
 * 打印请求参数及统计执行时长过滤器
 * 废除,该功能已迁移到CountTool
 *
 * @Version V1.0
 */
@Deprecated
//@Component
@Slf4j
public class CounterFilter implements GlobalFilter, XiehuaOrdered {


    @Autowired
    private ReactiveRedisTemplate<String, String> template;

    @Autowired
    private SimpleCache defaultCache;

    @Autowired
    private ObjectMapper mapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        String itemId = UUID.randomUUID().toString().replace("-", "");
        builder.header(HEAD_ITERM_ID, itemId);
        exchange.getResponse().getHeaders().put(HEAD_ITERM_ID, Arrays.asList(itemId));

        ServerHttpRequest request = builder.build();
        ServerWebExchange webExchange = exchange.mutate().request(request).response(exchange.getResponse()).build();

        webExchange.getAttributes().put(ATTR_REQ_ITEM, buildReqDTO(exchange, itemId));


        return chain.filter(webExchange);
    }

    private ReqDTO buildReqDTO(ServerWebExchange exchange, String itemId) {
        final String uri = exchange.getRequest().getURI().toString();
        final String method = Optional.ofNullable(exchange.getRequest().getMethod()).orElse(HttpMethod.GET).name();
        //final String headers = exchange.getRequest().getHeaders().entrySet().stream().map(s -> s.getKey() + ":[" + String.join(";", s.getValue()) + "]").collect(Collectors.joining("\r\n"));
        ReqDTO reqDTO = new ReqDTO();
        reqDTO.setReqId(itemId);
        String key = defaultCache.genKey(ATTR_REQ_ITEM + itemId);
        reqDTO.setUrl(uri);
        reqDTO.setMethod(method);
        reqDTO.setReqTime(LocalDateTime.now());
        return reqDTO;
    }

    @Override
    public int getOrder() {
        return COUNTER_ORDER;
    }

    private void asyncRW(String key, Span currentSpan) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusSeconds(EXP_SECONDS);
        Duration exp = Duration.between(now, end);

        template.opsForValue().get(key).switchIfEmpty(Mono.defer(() -> {
            try {
                return template.opsForValue().set(key, mapper.writeValueAsString(currentSpan), exp).then(Mono.empty());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return Mono.empty();
        })).flatMap(Try.of(s -> {
            Span rootSpan = mapper.readValue(s, new TypeReference<Span>() {
            });
            Span parentSpan = findParentSpan(rootSpan, currentSpan.getSpanId());
            if (parentSpan == null) return Mono.just(Boolean.FALSE);
            parentSpan.addChild(currentSpan);
            return template.opsForValue().set(key, mapper.writeValueAsString(rootSpan), exp);
        })).subscribe();

    }

    private Span findParentSpan(Span span, String spanId) {
        if (spanId.equals(span.getSpanId())) return span;
        List<Span> childs = span.getChilds();
        if (childs != null && childs.size() > 1) {
            for (int i = 0; i < childs.size(); i++) {
                Span temp = childs.get(i);
                if (spanId.equals(temp.getSpanId())) return temp;
                findParentSpan(temp, spanId);
            }
        }
        return null;
    }
}