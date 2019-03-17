package com.xiehua.config.secruity.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.bus.jvm.Bus;
import com.xiehua.fun.Try;
import io.vavr.Tuple2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JWTSecurityContextRepository implements ServerSecurityContextRepository {

    public static final String REDIS_GATEWAY_LOGIN_PREFIX = "gateway:login:tid_";//redis login info( jwt claims),the key is claims.id

    @Autowired
    private ReactiveRedisTemplate<String, String> template;

    @Autowired
    private ObjectMapper mapper;


    @Override
    public Mono<Void> save(ServerWebExchange serverWebExchange, SecurityContext securityContext) {
        serverWebExchange.getAttributes().put(REDIS_GATEWAY_LOGIN_PREFIX,securityContext.getAuthentication().getCredentials());
        Bus.post(securityContext);
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange serverWebExchange) {
        String key = REDIS_GATEWAY_LOGIN_PREFIX + (String)serverWebExchange.getAttributes().get(REDIS_GATEWAY_LOGIN_PREFIX);
        return template.opsForValue().get(key).switchIfEmpty(Mono.empty()).flatMap(Try.of(s ->Mono.just(mapper.readValue(s,SecurityContext.class))));
    }
}
