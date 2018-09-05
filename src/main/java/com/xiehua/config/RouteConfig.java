package com.xiehua.config;

import com.xiehua.filter.CpuRateLimitGatewayFilter;
import com.xiehua.filter.IpRateLimitGatewayFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static io.netty.handler.codec.http.cookie.CookieHeaderNames.MAX_AGE;
import static org.springframework.web.cors.CorsConfiguration.ALL;

@Configuration
public class RouteConfig {


    @Autowired
    private CpuRateLimitGatewayFilter cpuRateLimitGatewayFilter;

    @Autowired
    private IpRateLimitGatewayFilter ipGatewayFilter;

    @Bean
    public RouteLocator customerRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("service_order_center", r -> r.path("/order_center/**")
                        .filters(f -> f.stripPrefix(1)
                                .filter(ipGatewayFilter)
                                .filter(cpuRateLimitGatewayFilter)
                                //.retry(2)
                                .hystrix(s -> s.setFallbackUri("forward:/fallback"))
                        )
                        .uri("lb://ORDER-CENTER")
                ).route("service_pay_center", r -> r.path("/pay_center/**")
                        .filters(f -> f.stripPrefix(1)
                                .filter(ipGatewayFilter)
                                .filter(cpuRateLimitGatewayFilter)
                         //       .retry(2)
                        )
                        .uri("lb://PAY-CENTER"))
                .build();
    }

}
