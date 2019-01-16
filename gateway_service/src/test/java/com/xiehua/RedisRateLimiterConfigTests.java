package com.xiehua;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 */
//@RunWith(SpringRunner.class)
//@SpringBootTest
//@DirtiesContext
//@ActiveProfiles("redis-rate-limiter-config")
public class RedisRateLimiterConfigTests {

    @Autowired
    private RedisRateLimiter rateLimiter;

    @Autowired
    private RouteLocator routeLocator;

    @Before
    public void init() {
        routeLocator.getRoutes().collectList().block(); // prime routes since getRoutes() no longer blocks
    }

//    @Test
    public void redisRateConfiguredFromEnvironment() {
        assertFilter("redis_rate_limiter_config_test", 10, 20,
                false);
    }

//    @Test
    public void redisRateConfiguredFromJavaAPI() {
        assertFilter("custom_redis_rate_limiter", 20, 40,
                false);
    }

//    @Test
    public void redisRateConfiguredFromJavaAPIDirectBean() {
        assertFilter("alt_custom_redis_rate_limiter", 30, 60,
                true);
    }

    private void assertFilter(String key, int replenishRate, int burstCapacity,
                              boolean useDefaultConfig) {
        RedisRateLimiter.Config config;

        if (useDefaultConfig) {
           // config = rateLimiter.getDefaultConfig();
        } else {
            assertThat(rateLimiter.getConfig()).containsKey(key);
            config = rateLimiter.getConfig().get(key);
        }

        Route route = routeLocator.getRoutes().filter(r -> r.getId().equals(key)).next().block();
        assertThat(route).isNotNull();
        assertThat(route.getFilters()).hasSize(1);
    }

//    @EnableAutoConfiguration
//    @SpringBootConfiguration
    public static class TestConfig {

        @Bean
        public RouteLocator testRouteLocator(RouteLocatorBuilder builder) {
            return builder.routes()
                    .route("custom_redis_rate_limiter", r -> r.path("/custom")
                            .filters(f -> f.requestRateLimiter()
                                    .rateLimiter(RedisRateLimiter.class,
                                            rl -> rl.setBurstCapacity(40).setReplenishRate(20))
                                    .and())
                            .uri("http://localhost"))
                    .route("alt_custom_redis_rate_limiter", r -> r.path("/custom")
                            .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(myRateLimiter())))
                            .uri("http://localhost"))
                    .build();

        }

        @Bean
        public RedisRateLimiter myRateLimiter() {
            return new RedisRateLimiter(30, 60);
        }
    }
}