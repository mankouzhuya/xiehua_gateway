package com.xiehua.config.secruity.jwt;

import com.xiehua.config.dto.CustomConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;

import static com.xiehua.config.secruity.jwt.JWTSecurityContextRepository.REDIS_GATEWAY_LOGIN_PREFIX;

@Slf4j
@Component
public class JWTAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

    public static final String REDIS_GATEWAY_ONLINE_PREFIX = "gateway:online:account_";//redis login info( jwt claims),the key is user account

    @Autowired
    private CustomConfig customConfig;

    @Autowired
    private ReactiveRedisTemplate<String, String> template;

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        log.info("认证成功~");
        ServerWebExchange exchange = webFilterExchange.getExchange();
        XiehuaAuthenticationToken xiehuaAuthenticationToken = (XiehuaAuthenticationToken) authentication;
        Claims claims = xiehuaAuthenticationToken.getClaims();
        LocalDateTime exp = claims.getExpiration().toInstant().atZone(ZoneOffset.systemDefault()).toLocalDateTime();
        //访问退出登录接口
        if (exchange.getRequest().getURI().getRawPath().contains("logout")){
            String accunt = claims.getAudience();
            String gid = claims.getSubject();
            return template.opsForValue().delete(REDIS_GATEWAY_LOGIN_PREFIX + gid).then(template.delete(REDIS_GATEWAY_ONLINE_PREFIX + accunt)).then(webFilterExchange.getChain().filter(exchange));
        }

        //update response
        claims.setExpiration(Date.from(LocalDateTime.now().plusSeconds(customConfig.getJwtExpiration()).atZone(ZoneId.systemDefault()).toInstant()));
        exchange.getResponse().getHeaders().put(HttpHeaders.AUTHORIZATION, Arrays.asList(Jwts.builder().setClaims(claims).signWith(customConfig.getJwtSingKey()).compact()));
        return webFilterExchange.getChain().filter(exchange);
    }


}
