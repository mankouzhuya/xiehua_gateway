package com.xiehua.service;

import com.xiehua.config.dto.CustomConfig;
import com.xiehua.controller.dto.AddRuleReqDTO;
import com.xiehua.controller.dto.DeleteRuleReqDTO;
import com.xiehua.controller.dto.IndexRespDTO;
import com.xiehua.filter.IpRateLimitGatewayFilter;
import io.jsonwebtoken.Jwts;
import io.vavr.Tuple2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.xiehua.filter.IpRateLimitGatewayFilter.GATEWAY_RULE;

@Service
@Slf4j
@Validated
public class GateWayService {

    @Autowired
    private CustomConfig customConfig;

    @Autowired
    private ReactiveRedisTemplate<String, String> template;

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private IpRateLimitGatewayFilter filter;

    /**
     * 首页->登录用户,服务列表,路由信息
     * **/
    public IndexRespDTO index(@NotBlank(message = "认证信息不能为空") String authentication){
        IndexRespDTO respDTO = new IndexRespDTO(Jwts.parser().setSigningKey(customConfig.getJwtSingKey()).parseClaimsJws(authentication).getBody().getSubject());
        List<IndexRespDTO.Service> serviceList = routeLocator.getRoutes().filter(s -> s.getFilters().contains(filter)).publishOn(Schedulers.elastic()).map(s ->{
            IndexRespDTO.Service service = new IndexRespDTO.Service();
            String serviceName = s.getUri().getHost();
            List<IndexRespDTO.Rule> list = template.opsForHash().entries(GATEWAY_RULE + serviceName).map(m ->{
                String key = (String)m.getKey();
                String value = (String)m.getValue();
                String[] keys = key.split(":");
                String[] values = value.split(":");
                return new IndexRespDTO.Rule(keys[0],keys[1],values[0],values[1]);
            }).toStream().collect(Collectors.toList());
            service.setServiceName(serviceName);
            service.setRoles(list);
            return service;
        }).collectList().block();
        respDTO.setServiceList(serviceList);
        return respDTO;
    }

    /**
     * 添加路由规则
     * **/
    public Mono<Boolean> addRule(@Validated @RequestBody AddRuleReqDTO addRuleReqDTO){
        return template.opsForHash().put(GATEWAY_RULE + addRuleReqDTO.getService(),addRuleReqDTO.getKey(),addRuleReqDTO.getValue());
    }

    /**
     * 删除路由规则
     * **/
    public Mono<Long> deleteRule(@Validated @RequestBody DeleteRuleReqDTO deleteRuleReqDTO) {
        return template.opsForHash().remove(GATEWAY_RULE + deleteRuleReqDTO.getService(),deleteRuleReqDTO.getKey());
    }

}
