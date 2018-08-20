package com.xiehua.config;

import com.xiehua.filter.CpuRateLimitGatewayFilter;
import com.xiehua.filter.IpRateLimitGatewayFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class RouteConfig {

    @Autowired
    private CpuRateLimitGatewayFilter cpuRateLimitGatewayFilter;

    @Autowired
    private IpRateLimitGatewayFilter ipGatewayFilter;

    @Bean
    public RouteLocator customerRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(r -> r.path("/order_center/**")
                        .filters(f -> f.stripPrefix(1)
                                .filter(ipGatewayFilter)
                                .filter(cpuRateLimitGatewayFilter)
                                .retry(2)
                                .hystrix(s -> s.setFallbackUri("forward:/fallback"))
                                .addResponseHeader("X-Response-Gateway", UUID.randomUUID().toString().replace("-",""))
                        )
                        .uri("lb://ORDER-CENTER")
                        .order(0)
                        .id("order_center_service")
                )
                .build();
    }
}
