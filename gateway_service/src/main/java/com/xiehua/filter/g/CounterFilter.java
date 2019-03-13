package com.xiehua.filter.g;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.cache.SimpleCache;
import com.xiehua.filter.XiehuaOrdered;
import com.xiehua.support.wrap.dto.ReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.xiehua.filter.RouteFilter.HEAD_ITERM_ID;

/**
 * 打印请求参数及统计执行时长过滤器
 * @Version V1.0
 */
@Component
@Slf4j
public class CounterFilter implements GlobalFilter, XiehuaOrdered {


    private static final String GATEWAY_ATTR_REQ_TIME = "gateway_attr_req_time";//发起请求时间

    private static final String GATEWAY_ATTR_RECEIVE_REQ_TIME = "gateway_attr_receive_req_time";//收到请求时间

    public static final String REDIS_GATEWAY_TIMER_REQID_PREFIX = "gateway:timer:count_";

    public static final String ATTR_REQ_ITEM = "attr_req_item";

    public static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final Integer DEFAULT_TIMER=  2000;//接口耗时时长（2秒钟）



    @Autowired
    private SimpleCache defaultCache;

    @Autowired
    private ObjectMapper mapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        String itemId = UUID.randomUUID().toString().replace("-","");
        builder.header(HEAD_ITERM_ID, itemId);
        exchange.getResponse().getHeaders().put(HEAD_ITERM_ID, Arrays.asList(itemId));

        ServerHttpRequest request = builder.build();
        ServerWebExchange webExchange = exchange.mutate().request(request).response(exchange.getResponse()).build();

        webExchange.getAttributes().put(GATEWAY_ATTR_REQ_TIME, System.currentTimeMillis());
        webExchange.getAttributes().put(ATTR_REQ_ITEM,buildReqDTO(exchange,itemId));

        try {
            ReqDTO reqDTO = buildReqDTO(exchange,itemId);
            defaultCache.put(reqDTO.getKey(),mapper.writeValueAsString(reqDTO));
        } catch (JsonProcessingException e) {
            log.error("序列化失败:{}",e);
        }

        return chain.filter(webExchange);
    }

    private ReqDTO buildReqDTO(ServerWebExchange exchange, String itemId) {
        final String uri = exchange.getRequest().getURI().toString();
        final String method = Optional.ofNullable(exchange.getRequest().getMethod()).orElse(HttpMethod.GET).name();
        final String headers = exchange.getRequest().getHeaders().entrySet().stream().map(s -> s.getKey() + ":[" + String.join(";", s.getValue()) + "]").collect(Collectors.joining("\r\n"));
        ReqDTO reqDTO = new ReqDTO();
        reqDTO.setReqId(itemId);
        String key = defaultCache.genKey(ATTR_REQ_ITEM + itemId);
        reqDTO.setKey(key);
        reqDTO.setUrl(uri);
        reqDTO.setMethod(method);
        reqDTO.setHead(headers);
        reqDTO.setReqTime(LocalDateTime.now());
        return reqDTO;
    }

    @Override
    public int getOrder() {
        return COUNTER_ORDER;
    }
}