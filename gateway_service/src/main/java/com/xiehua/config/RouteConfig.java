package com.xiehua.config;

import com.xiehua.filter.RateLimitCpuFilter;
import com.xiehua.filter.RateLimitIpFilter;
import com.xiehua.filter.RouteFilter;
import com.xiehua.filter.TrackFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {


    @Autowired
    private RateLimitCpuFilter rateLimitCpuFilter;

    @Autowired
    private RateLimitIpFilter rateLimitIpFilter;

    @Autowired
    private TrackFilter trackFilter;

    @Autowired
    private RouteFilter routeFilter;//必选


    @Bean
    public RouteLocator customerRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("service_order_center", r -> r.path("/order_center/**")
                        .filters(f -> f.stripPrefix(1)
//                                .filter(rateLimitCpuFilter)
                                .filter(rateLimitIpFilter)
                                .filter(trackFilter)
                                .filter(routeFilter)
                                //.retry(2)
                                .hystrix(s -> s.setFallbackUri("forward:/gateway/fallback"))
                        )
                        .uri("lb://ORDER-CENTER")
                ).route("service_pay_center", r -> r.path("/pay_center/**")
                        .filters(f -> f.stripPrefix(1)
//                                        .filter(rateLimitCpuFilter)
                                        .filter(rateLimitIpFilter)
                                        .filter(trackFilter)
                                        .filter(routeFilter)
                                //       .retry(2)
                        )
                        .uri("lb://PAY-CENTER"))
                .build();
    }

}
