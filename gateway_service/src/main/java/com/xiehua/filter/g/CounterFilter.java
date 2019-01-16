package com.xiehua.filter.g;
import com.xiehua.filter.XiehuaOrdered;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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

    public static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Integer DEFAULT_TIMER=  2000;//接口耗时时长（2秒钟）

    @Autowired
    private ReactiveRedisTemplate<String, String> template;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        String itemId = UUID.randomUUID().toString().replace("-","");
        builder.header(HEAD_ITERM_ID, itemId);
        ServerHttpRequest request = builder.build();
        ServerWebExchange webExchange = exchange.mutate().request(request).response(exchange.getResponse()).build();

        webExchange.getAttributes().put(GATEWAY_ATTR_REQ_TIME, System.currentTimeMillis());
        webExchange.getAttributes().put(GATEWAY_ATTR_RECEIVE_REQ_TIME, LocalDateTime.now());
        return chain.filter(webExchange).then( Mono.fromRunnable(() -> {
            Long startTime = webExchange.getAttribute(GATEWAY_ATTR_REQ_TIME);
            if (startTime != null) {
                Long executeTime = (System.currentTimeMillis() - startTime);
                String requestInfo = String.format("处理请求->URL:{%s} Method:{%s} 请求时间:{%s} 处理耗时:{%s} ms",
                        webExchange.getRequest().getURI().toString(),
                        webExchange.getRequest().getMethod().name(),
                        webExchange.getAttribute(GATEWAY_ATTR_RECEIVE_REQ_TIME),
                        executeTime);
                log.info(requestInfo);
                if(executeTime > DEFAULT_TIMER){//记录耗时较长的接口
                    String key = REDIS_GATEWAY_TIMER_REQID_PREFIX + LocalDate.now().toString();
                    template.opsForHash()
                            .put(key,itemId,requestInfo)
                            .then(template.expire(key, Duration.between(LocalDateTime.now(), LocalDateTime.parse( LocalDate.now().plusDays(7).toString() + " 23:59:59",TIME_FORMATTER))))
                            .subscribe();

                }
            }
        }));
    }

    @Override
    public int getOrder() {
        return COUNTER_ORDER;
    }
}