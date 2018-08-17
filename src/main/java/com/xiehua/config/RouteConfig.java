package com.xiehua.config;

import com.xiehua.filter.RateLimitByCpuGatewayFilter;
import com.xiehua.filter.RateLimitByIpGatewayFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Autowired
    private RateLimitByCpuGatewayFilter rateLimitByCpuGatewayFilter;

    @Autowired
    private RateLimitByIpGatewayFilter ipGatewayFilter;

    @Bean
    public RouteLocator customerRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(r -> r.path("/order_center/**")
                        .filters(f -> f.stripPrefix(1)
                                .filter(ipGatewayFilter)
                               // .filter(rateLimitByCpuGatewayFilter)
                                .retry(2)

                                .hystrix(s -> s.setFallbackUri("forward:/fallback"))
                                .addResponseHeader("X-Response-Default-Foo", "Default-Bar")
                        )
                        .uri("lb://ORDER-CENTER")
                        .order(0)
                        .id("throttle_customer_service")
                )
                .build();
    }
}
