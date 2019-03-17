package com.xiehua.filter;

import com.xiehua.config.dto.CustomConfig;
import com.xiehua.config.secruity.jwt.converter.ServerHttpBearerAuthenticationConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class JwtAuthenticationFilter extends Authenticcation implements WebFilter {

    private ServerHttpBearerAuthenticationConverter converter;

    public JwtAuthenticationFilter(CustomConfig config,ServerHttpBearerAuthenticationConverter converter,ReactiveAuthenticationManager authenticationManager, ServerSecurityContextRepository securityContextRepository, ServerAuthenticationSuccessHandler authenticationSuccessHandler) {
        super(config,authenticationManager, securityContextRepository, authenticationSuccessHandler);
        this.converter = converter;
    }


    /**
     * Process the Web request and (optionally) delegate to the next
     * {@code WebFilter} through the given {@link WebFilterChain}.
     *
     * @param exchange the current server exchange
     * @param chain    provides a way to delegate to the next filter
     * @return {@code Mono<Void>} to indicate when request processing is complete
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if(checkPermitUrls(exchange)) return chain.filter(exchange);
        return converter.apply(exchange).publishOn(Schedulers.elastic()).flatMap(i -> authenticate(exchange, chain, i));
    }
}
