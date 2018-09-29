package com.xiehua.service;

import com.xiehua.config.dto.CustomConfig;
import com.xiehua.config.dto.jwt.JwtUser;
import com.xiehua.controller.dto.*;
import com.xiehua.filter.RateLimitIpFilter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.xiehua.converter.ServerHttpBearerAuthenticationConverter.REDIS_GATEWAY_USER_POWER_PREFIX;
import static com.xiehua.filter.RouteFilter.*;
import static com.xiehua.filter.g.CounterFilter.REDIS_GATEWAY_TIMER_REQID_PREFIX;


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
    private RateLimitIpFilter filter;

    /**
     * 首页->登录用户,服务列表,路由信息
     **/
    public IndexRespDTO index(@NotBlank(message = "认证信息不能为空") String authentication) {
        IndexRespDTO respDTO = new IndexRespDTO(Jwts.parser().setSigningKey(customConfig.getJwtSingKey()).parseClaimsJws(authentication).getBody().getSubject());
        List<IndexRespDTO.Service> serviceList = routeLocator.getRoutes().filter(s -> s.getFilters().contains(filter)).publishOn(Schedulers.elastic()).map(s -> {
            IndexRespDTO.Service service = new IndexRespDTO.Service();
            String serviceName = s.getUri().getHost();
            List<IndexRespDTO.Rule> list = template.opsForHash().entries(REDIS_GATEWAY_SERVICE_RULE + serviceName).map(m -> {
                String key = (String) m.getKey();
                String value = (String) m.getValue();
                String[] keys = key.split(":");
                String[] values = value.split(":");
                return new IndexRespDTO.Rule(keys[0], keys[1], values[0], values[1]);
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
     **/
    public Mono<Boolean> addRule(@Validated @RequestBody AddRuleReqDTO addRuleReqDTO) {
        return template.opsForHash().put(REDIS_GATEWAY_SERVICE_RULE + addRuleReqDTO.getService(), addRuleReqDTO.getKey(), addRuleReqDTO.getValue());
    }

    /**
     * 删除路由规则
     **/
    public Mono<Long> deleteRule(@Validated @RequestBody DeleteRuleReqDTO deleteRuleReqDTO) {
        return template.opsForHash().remove(REDIS_GATEWAY_SERVICE_RULE + deleteRuleReqDTO.getService(), deleteRuleReqDTO.getKey());
    }

    /**
     * 某个用户是否在线
     **/
    public Mono<Boolean> isOnLine(String account) {
        return template.hasKey(REDIS_GATEWAY_ONLINE_PREFIX + account);
    }

    /**
     * 在线人数统计
     **/
    public Mono<Long> getOnLinePersion() {
        return template.keys(REDIS_GATEWAY_ONLINE_PREFIX + "*").count();
    }

    /**
     * 获取当前登录用户信息
     **/
    public Mono<JwtUser> getCurrentLoginUser(@NotBlank(message = "tid不能为空") String tid) {
        return template.opsForHash().entries(REDIS_GATEWAY_LOGIN_PREFIX + tid)
                .collectMap(x -> x.getKey().toString(), y -> y.getValue().toString())
                .map(s -> new JwtUser(s.get(Claims.SUBJECT), s.get(Claims.AUDIENCE), s.get(Claims.ID), s.get(Claims.ISSUER), null));
    }

    /**
     * 查询用户权限
     **/
    public Mono<UserPermissionsRespDTO> getUserPermissionsList(@NotBlank(message = "用户账号account不能为空") String account) {
        return template.opsForHash().entries(REDIS_GATEWAY_USER_POWER_PREFIX + account).map(s -> {
            String key = s.getKey().toString();
            String value = s.getValue().toString();
            PermissionsSystem system = new PermissionsSystem();
            system.setSystem(key);
            system.setPermissions(Arrays.asList(value.split(",")));
            return system;
        }).collectList().map(s -> new UserPermissionsRespDTO(account, s));
    }

    /**
     * 添加用户权限(存在即更新,全量替换)
     * 注意:次接口是全量替换用户对应系统的权限标识,故调用的时候一定要传用户在对应系统的全部权限标识
     **/
    public Mono<Void> addPermissions(@Validated AddPermissionsReqDTO addPermissionsReqDTO) {
        return template.opsForHash().put(REDIS_GATEWAY_USER_POWER_PREFIX + addPermissionsReqDTO.getAccount(), addPermissionsReqDTO.getSys(), String.join(",", addPermissionsReqDTO.getPermissions())).thenEmpty(Mono.empty());
    }

    /**
     * 添加用户权限(存在即更新,全量替换)
     * 注意:次接口是全量替换用户对应系统的权限标识,故调用的时候一定要传用户在对应系统的全部权限标识
     **/
    public Mono<Void> addPermissions(@Validated AddPermissionsReq2DTO addPermissionsReqDTO) {
        return template.opsForHash().put(REDIS_GATEWAY_USER_POWER_PREFIX + addPermissionsReqDTO.getAccount(), addPermissionsReqDTO.getSys(), addPermissionsReqDTO.getPermissions()).thenEmpty(Mono.empty());
    }

    /**
     * 耗时长连接统计
     **/
    public CountIndexRespDTO countIndexRespDTO(String authentication) {
        return template.opsForHash().entries(REDIS_GATEWAY_TIMER_REQID_PREFIX + LocalDate.now().toString())
                .map(s -> new CountRowDTO(s.getKey().toString(), s.getValue().toString()))
                .collectList()
                .map(s -> new CountIndexRespDTO(Jwts.parser().setSigningKey(customConfig.getJwtSingKey()).parseClaimsJws(authentication).getBody().getSubject(), s)).block();
    }
}
