package com.xiehua.config.secruity.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.bus.jvm.Bus;
import com.xiehua.fun.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.xiehua.config.dto.white_list.WhiteListPermit.DEFAULT_WHITE_GID;
import static com.xiehua.config.dto.white_list.WhiteListPermit.GATEWAY_LOGIN_ACCOUNT;

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
        serverWebExchange.getAttributes().put(REDIS_GATEWAY_LOGIN_PREFIX, securityContext.getAuthentication().getCredentials());
        String credentials = (String) securityContext.getAuthentication().getCredentials();
        if (!DEFAULT_WHITE_GID.equals(credentials) && !GATEWAY_LOGIN_ACCOUNT.equals(credentials)) Bus.post(securityContext);
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange serverWebExchange) {
        String key = REDIS_GATEWAY_LOGIN_PREFIX + (String) serverWebExchange.getAttributes().get(REDIS_GATEWAY_LOGIN_PREFIX);
        return template.opsForValue().get(key).switchIfEmpty(Mono.empty()).flatMap(Try.of_f(s -> Mono.just(mapper.readValue(s, SecurityContext.class))));
    }
}
