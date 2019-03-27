package com.xiehua.task;

import com.xiehua.cache.SimpleCache;
import com.xiehua.filter.RateLimitIpFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

import static com.xiehua.filter.RouteFilter.REDIS_GATEWAY_SERVICE_RULE;

@Slf4j
@Component
public class SynRedisTask {

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private RateLimitIpFilter filter;

    @Autowired
    private ReactiveRedisTemplate<String, String> template;

    @Autowired
    private SimpleCache defaultCache;

    @Scheduled(cron="0 0/10 * * * ?")//10分钟一次
    public void synRoute(){
        log.info("开始同步路由规则...");
        ReactiveHashOperations<String, String, String> ops =  template.opsForHash();
        routeLocator.getRoutes().filter(s -> s.getFilters().contains(filter)).publishOn(Schedulers.elastic()).map(s -> {
            String serviceName = s.getUri().getHost();
            String serviceKey = REDIS_GATEWAY_SERVICE_RULE + serviceName;
            Map<String,String> rules = ops.entries(serviceKey).collectMap(k -> k.getKey(), v -> v.getValue()).block();
            return Mono.just(defaultCache.put(defaultCache.genKey(serviceKey), rules));
        }).subscribe();
    }
}
