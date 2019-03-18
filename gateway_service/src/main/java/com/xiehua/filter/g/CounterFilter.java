package com.xiehua.filter.g;

import com.xiehua.filter.XiehuaOrdered;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange);
    }



    @Override
    public int getOrder() {
        return COUNTER_ORDER;
    }


}