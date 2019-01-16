package com.xiehua.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.cache.dto.SimpleKvDTO;
import com.xiehua.config.dto.CustomConfig;
import com.xiehua.config.dto.jwt.JwtUser;
import com.xiehua.controller.dto.*;
import com.xiehua.filter.RateLimitIpFilter;
import com.xiehua.pub_sub.redis.dto.XiehuaMessage;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.xiehua.cache.DefaultCache.REDIS_GATEWAY_UPDATE_LOCALCACHE_TOPIC;
import static com.xiehua.converter.ServerHttpBearerAuthenticationConverter.REDIS_GATEWAY_USER_POWER_PREFIX;
import static com.xiehua.filter.RouteFilter.*;
import static com.xiehua.filter.g.CounterFilter.REDIS_GATEWAY_TIMER_REQID_PREFIX;
import static com.xiehua.pub_sub.redis.dto.XiehuaMessage.*;


@Service
@Slf4j
@Validated
public class GateWayService {

    @Autowired
    private CustomConfig customConfig;

    @Autowired
    private ReactiveRedisTemplate<String, String> template;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private RateLimitIpFilter filter;

    @Autowired
    private ObjectMapper mapper;

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
     * 路由规则---------->添加路由规则
     **/
    public Mono<Boolean> addRule(@Validated @RequestBody AddRuleReqDTO addRuleReqDTO) {
        return template.opsForHash().put(REDIS_GATEWAY_SERVICE_RULE + addRuleReqDTO.getService(), addRuleReqDTO.getKey(), addRuleReqDTO.getValue())
                .publishOn(Schedulers.elastic())
                .then(Mono.fromRunnable(() -> {
                    try {
                        notifyUpdateRule(addRuleReqDTO.getService());
                    } catch (JsonProcessingException e) {
                        log.error("路由规则---------->删除路由规则 service:{},堆栈:{}", addRuleReqDTO.getService(), e);
                    }
                }));//notify update local cache
    }

    /**
     * 路由规则(其中的一个一条)---------->删除路由规则
     **/
    public Mono<Long> deleteRule(@Validated @RequestBody DeleteRuleReqDTO deleteRuleReqDTO) {
        return template.opsForHash().remove(REDIS_GATEWAY_SERVICE_RULE + deleteRuleReqDTO.getService(), deleteRuleReqDTO.getKey())
                .publishOn(Schedulers.elastic())
                .then(Mono.fromRunnable(() -> {
                    try {
                        notifyUpdateRule(deleteRuleReqDTO.getService());
                    } catch (JsonProcessingException e) {
                        log.error("路由规则---------->删除路由规则 service:{},堆栈:{}", deleteRuleReqDTO.getService(), e);
                    }
                }));//notify update local cache
    }

    /**
     * 路由规则(全部删除)---------->删除路由规则
     **/
    public Mono<Long> deleteRule(String serviceId) {
        return template.opsForHash().delete(serviceId)
                .publishOn(Schedulers.elastic())
                .then(Mono.fromRunnable(() -> {
                    try {
                        stringRedisTemplate.convertAndSend(REDIS_GATEWAY_UPDATE_LOCALCACHE_TOPIC, mapper.writeValueAsString(new XiehuaMessage(TYPE_DELETE_RULE, serviceId)));
                    } catch (JsonProcessingException e) {
                        log.error("路由规则---------->删除路由规则 service:{},堆栈:{}", serviceId, e);
                    }
                }));//notify delete local cache
    }

    private void notifyUpdateRule(String service) throws JsonProcessingException {
        //query redis
        ReactiveHashOperations<String,String,String> opsForHash = template.opsForHash();
        List<SimpleKvDTO> rules = opsForHash.entries(service).map(s->{
            SimpleKvDTO dto = new SimpleKvDTO();
            dto.setKey(s.getKey());
            dto.setValue(s.getValue());
            return dto;
        }).collectList().block();

        AddRule2ReqDTO addRule2ReqDTO = new AddRule2ReqDTO();
        addRule2ReqDTO.setRules(rules);
        addRule2ReqDTO.setService(service);

        if(CollectionUtils.isEmpty(rules)) return;
        stringRedisTemplate.convertAndSend(REDIS_GATEWAY_UPDATE_LOCALCACHE_TOPIC, mapper.writeValueAsString(new XiehuaMessage(TYPE_UPDATE_RULE, mapper.writeValueAsString(addRule2ReqDTO))));
    }


    /**
     * 清空本地缓存
     * **/
    public void clearLocalCache() throws JsonProcessingException {
        stringRedisTemplate.convertAndSend(REDIS_GATEWAY_UPDATE_LOCALCACHE_TOPIC,mapper.writeValueAsString(new XiehuaMessage(TYPE_CLEAR_ALL, "clear all cache")));
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
     * 用户------>查询用户权限
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
     * 用户------>添加用户权限(存在即更新,全量替换)
     * 注意:次接口是全量替换用户对应系统的权限标识,故调用的时候一定要传用户在对应系统的全部权限标识
     **/
    public Mono<Void> addOrUpdatePermissions(@Validated AddPermissionsReqDTO addPermissionsReqDTO) {
        return template.opsForHash().put(REDIS_GATEWAY_USER_POWER_PREFIX + addPermissionsReqDTO.getAccount(), addPermissionsReqDTO.getSys(), String.join(",", addPermissionsReqDTO.getPermissions()))
                .publishOn(Schedulers.elastic())
                .then(Mono.fromRunnable(() -> {
                    try {
                        stringRedisTemplate.convertAndSend(REDIS_GATEWAY_UPDATE_LOCALCACHE_TOPIC, mapper.writeValueAsString(new XiehuaMessage(TYPE_UPDATE_USER_INFO, mapper.writeValueAsString(addPermissionsReqDTO))));
                    } catch (JsonProcessingException e) {
                        log.error("用户------>添加用户权限(存在即更新,全量替换)失败addPermissionsReqDTO:{},堆栈:{}",addPermissionsReqDTO.toString(),e);
                    }
                }));//notify update local cache

    }

    /**
     * 用户------>删除用户
     **/
    public Mono<Void> deleteUser(@NotBlank(message = "用户账号account不能为空") String account) {
        return template.delete(REDIS_GATEWAY_USER_POWER_PREFIX + account)
                .publishOn(Schedulers.elastic())
                .then(Mono.fromRunnable(() -> {
                    try {
                        stringRedisTemplate.convertAndSend(REDIS_GATEWAY_UPDATE_LOCALCACHE_TOPIC, mapper.writeValueAsString(new XiehuaMessage(TYPE_DELETE_USER_INFO, account)));
                    } catch (JsonProcessingException e) {
                        log.error("用户------>添加用户权限(存在即更新,全量替换)account:{},堆栈:{}",account,e);
                    }
                }));//notify delete local cache
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
