package com.xiehua.handle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JWTAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(JWTAuthenticationSuccessHandler.class);


    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        //TODO refactor this nasty implementation
//        exchange.getResponse()
//                .getHeaders()
//                .add(HttpHeaders.AUTHORIZATION, getHttpAuthHeaderValue(authentication));
        logger.info("Token认证成功");
        return webFilterExchange.getChain().filter(exchange);
    }

    private static String getHttpAuthHeaderValue(Authentication authentication) {
        return String.join(" ", "Bearer", tokenFromAuthentication(authentication));
    }

    private static String tokenFromAuthentication(Authentication authentication) {
        //jwtTokenComponent.generateToken();
//        return new JWTTokenService().generateToken(
//                authentication.getName(),
//                authentication.getCredentials(),
//                authentication.getAuthorities());
        return null;
    }

}
