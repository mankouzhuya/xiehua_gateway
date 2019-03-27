package com.xiehua.config.secruity.jwt;

import com.xiehua.config.dto.CustomConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;

import static com.xiehua.config.secruity.jwt.JWTSecurityContextRepository.REDIS_GATEWAY_LOGIN_PREFIX;
import static com.xiehua.config.secruity.jwt.converter.ServerHttpBearerAuthenticationConverter.BEARER;

@Slf4j
@Component
public class JWTAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

    public static final String REDIS_GATEWAY_ONLINE_PREFIX = "gateway:online:account_";//redis login info( jwt claims),the key is user account

    @Autowired
    private CustomConfig customConfig;

    @Value("${jwt.domain}")
    private String domain;

    @Autowired
    private ReactiveRedisTemplate<String, String> template;

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        XiehuaAuthenticationToken xiehuaAuthenticationToken = (XiehuaAuthenticationToken) authentication;
        Claims claims = xiehuaAuthenticationToken.getClaims();
        //访问退出登录接口
        if (exchange.getRequest().getURI().getRawPath().contains("logout")){
            String accunt = claims.getAudience();
            String gid = claims.getSubject();
            return template.opsForValue().delete(REDIS_GATEWAY_LOGIN_PREFIX + gid).then(template.delete(REDIS_GATEWAY_ONLINE_PREFIX + accunt)).then(webFilterExchange.getChain().filter(exchange));
        }
        if(!StringUtils.isEmpty(webFilterExchange.getExchange().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))){
            //update response
            Date exp = Date.from(LocalDateTime.now().plusSeconds(customConfig.getJwtExpiration()).atZone(ZoneId.systemDefault()).toInstant());
            claims.setExpiration(exp);
            String token = Jwts.builder().setClaims(claims).signWith(customConfig.getJwtSingKey()).compact();
            exchange.getResponse().getHeaders().put(HttpHeaders.AUTHORIZATION, Arrays.asList(BEARER + token));
            ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(HttpHeaders.AUTHORIZATION, token)
                    .httpOnly(true)
                    .domain(domain)
                    .path("/")
                    .maxAge(customConfig.getJwtExpiration());
            exchange.getResponse().addCookie(cookieBuilder.build());
        }

        return webFilterExchange.getChain().filter(exchange);
    }


}
