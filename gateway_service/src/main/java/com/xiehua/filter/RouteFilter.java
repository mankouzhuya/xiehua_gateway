package com.xiehua.filter;

import com.xiehua.cache.SimpleCache;
import com.xiehua.config.enums.ClientField;
import com.xiehua.exception.BizException;
import io.vavr.Tuple2;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.xiehua.component.GateWayComponent.readReq2Map;
import static com.xiehua.filter.Authenticcation.GATEWAY_ATTR_SERVER_NAME;


@Slf4j
@Component
public class RouteFilter implements GatewayFilter, XiehuaOrdered {

    public static final String REDIS_GATEWAY_SERVICE_RULE = "gateway:rules:service_";//redis service route rules

    public static final String REDIS_GATEWAY_RULE_DEFAULT = "default:default";//default route rule

    public static final String GATEWAY_ATTR_ROUTE_RULES = "gateway_attr_route_rules";// gateway attr route rules

    @Autowired
    private ReactiveRedisTemplate<String, String> template;

    @Autowired
    private SimpleCache defaultCache;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String serviceName = (String) exchange.getAttributes().get(GATEWAY_ATTR_SERVER_NAME);
        return Mono.just(exchange)
                .publishOn(Schedulers.elastic())
                .flatMap(m -> {
                    m.getAttributes().put(GATEWAY_ATTR_ROUTE_RULES, queryRouteInfo(m, serviceName));
                    return chain.filter(m);
                });
    }


    @Override
    public int getOrder() {
        return ROUTE_PRE_ORDER;
    }


    /**
     * query route from redis
     **/
    private Map<String, String> queryRouteInfo(ServerWebExchange exchange, String serviceName) {
        //request info
        Map<String, String> map = readReq2Map(exchange);
        List<String> list = Arrays.asList(ClientField.values()).stream().map(s -> {
            String field = s.getValue();
            String value = map.get(field);
            if (StringUtils.isEmpty(value)) return null;
            return field + ":" + value;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        if (list == null || list.size() < 1) list.add(REDIS_GATEWAY_RULE_DEFAULT);
        //load rules
        String serviceKey = REDIS_GATEWAY_SERVICE_RULE + serviceName;
        Map<String, String> ruleMap = loadLocalCache(serviceKey);
        Map<String, String> fmap = list
                .stream()
                .filter(s -> ruleMap.containsKey(s))
                .map(v -> {
                    String val = ruleMap.get(v);
                    String[] temp = val.split(":");
                    Tuple2<String, String> t = new Tuple2(temp[0], temp[1]);
                    return t;
                }).collect(Collectors.toMap(m -> m._1, n -> n._2, (x, y) -> y));
        if (CollectionUtils.isEmpty(fmap)) {
            log.error(serviceKey + "路由规则未配置(redis):" + list.toString());
            throw new BizException(serviceKey + "路由规则未配置(redis):" + list.toString());
        }
        return fmap;
    }


    //load config form local cache
    private Map<String, String> loadLocalCache(String service) {
        //query local cache
        String localKey = defaultCache.genKey(service);
        Object localValue = defaultCache.get(localKey);
        if (!Objects.isNull(localValue)) {
            Map<String, String> rules = (Map<String, String>) localValue;
            return rules;
        }
        //query redis
        ReactiveHashOperations<String, String, String> opsForHash = template.opsForHash();
        Map<String, String> rules = opsForHash.entries(service).collectMap(s -> s.getKey(), t -> t.getValue()).block();
        if (CollectionUtils.isEmpty(rules)) {
            log.error(service + "路由规则未配置(redis)");
            throw new RuntimeException(service + "路由规则未配置(redis)");
        }
        defaultCache.put(localKey, rules);
        return rules;
    }


}
