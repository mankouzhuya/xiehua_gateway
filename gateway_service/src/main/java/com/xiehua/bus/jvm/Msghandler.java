package com.xiehua.bus.jvm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import com.xiehua.config.secruity.jwt.XiehuaAuthenticationToken;
import com.xiehua.fun.Try;
import com.xiehua.support.wrap.collect.CountTool;
import com.xiehua.support.wrap.collect.TrackTool;
import com.xiehua.support.wrap.dto.ReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.xiehua.config.secruity.jwt.JWTAuthenticationSuccessHandler.REDIS_GATEWAY_ONLINE_PREFIX;
import static com.xiehua.config.secruity.jwt.JWTSecurityContextRepository.REDIS_GATEWAY_LOGIN_PREFIX;
import static com.xiehua.support.wrap.dto.ReqDTO.*;

@Slf4j
public class Msghandler {

    private ReactiveRedisTemplate<String, String> template;

    private ObjectMapper mapper;

    private CountTool countTool;

    private TrackTool trackTool;

    private Msghandler() {
    }

    ;

    public Msghandler(ReactiveRedisTemplate<String, String> template, ObjectMapper mapper, CountTool countTool, TrackTool trackTool) {
        this.template = template;
        this.mapper = mapper;
        this.countTool = countTool;
        this.trackTool = trackTool;
    }


    /**
     * 只有通过@Subscribe注解的方法才会被注册进EventBus
     * 而且方法有且只能有1个参数
     *
     * @param reqDTO
     */
    @Subscribe
    public void process(ReqDTO reqDTO) throws JsonProcessingException, ExecutionException, InterruptedException {
        if (TYPE_SAVE_TEMP.equals(reqDTO.getType())) {//临时保存ReqDTO
            saveTempReqDTO(reqDTO);
            return;
        }
        if (TYPE_COUNT_EXEC_TIME.equals(reqDTO.getType())) {//计算是否超时并持久化
            countTool.countExecuteTime(reqDTO);
            return;
        }
    }

    @Subscribe
    public void process(SecurityContext securityContext) throws JsonProcessingException, ExecutionException, InterruptedException {
        XiehuaAuthenticationToken xiehuaAuthenticationToken = (XiehuaAuthenticationToken) securityContext.getAuthentication();
        LocalDateTime exp = xiehuaAuthenticationToken.getClaims().getExpiration().toInstant().atZone(ZoneOffset.systemDefault()).toLocalDateTime();
        String key = REDIS_GATEWAY_LOGIN_PREFIX + (String) securityContext.getAuthentication().getCredentials();
        LocalDateTime now = LocalDateTime.now();
        String accunt = xiehuaAuthenticationToken.getClaims().getAudience();
        String gid = xiehuaAuthenticationToken.getClaims().getSubject();
        template.opsForValue()
                .set(key, mapper.writeValueAsString(securityContext), Duration.between(now, exp))
                .then(template.opsForValue().set(REDIS_GATEWAY_ONLINE_PREFIX + accunt, gid, Duration.between(now, exp))).subscribe();
    }


    public void saveTempReqDTO(ReqDTO reqDTO) throws JsonProcessingException {
        String key = REDIS_GATEWAY_TEMP_PREFIX + reqDTO.getTrackId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusSeconds(REDIS_GATEWAY_TEMP_EXP);
        template.opsForHash()
                .put(key, reqDTO.getReqId(), mapper.writeValueAsString(reqDTO))
                .then(template.expire(key, Duration.between(now, end)))
                .then(Mono.just(reqDTO))
                .flatMap(Try.of(s -> {
                    Map<String, Object> map = s.getBizMap();
                    if (map != null && map.get("reqOrder") != null) trackTool.track(reqDTO);
                    return Mono.empty();
                })).subscribe();
    }
}
