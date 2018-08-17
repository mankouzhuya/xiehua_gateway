package com.xiehua.authentication;

import com.xiehua.config.dto.CustomConfig;
import com.xiehua.converter.ServerHttpBearerAuthenticationConverter;
import com.xiehua.jwt.JWTReactiveAuthenticationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.WebFilterChainServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.xiehua.secruity.GateWaySecurity.URL_PERMIT_ALL;


public class JwtAuthenticationFilter implements WebFilter {

    private static final Logger logger =  LoggerFactory.getLogger(JwtAuthenticationFilter.class);


    private ServerHttpBearerAuthenticationConverter converter;

    private CustomConfig config;

    public JwtAuthenticationFilter(CustomConfig config,ServerHttpBearerAuthenticationConverter converter){
        this.config = config;
        this.converter = converter;
    }


    private final ReactiveAuthenticationManager authenticationManager = new JWTReactiveAuthenticationManager();

    private ServerSecurityContextRepository securityContextRepository = NoOpServerSecurityContextRepository.getInstance();

    private ServerAuthenticationSuccessHandler authenticationSuccessHandler = new WebFilterChainServerAuthenticationSuccessHandler();

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

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
        if(logger.isDebugEnabled()) logger.debug("收到请求:",exchange.getRequest().getURI().toString());
        //OPTIONS 请求不处理
        if(exchange.getRequest().getMethod().equals(HttpMethod.OPTIONS)) return exchange.getResponse().setComplete();
        //permit all url
        List<String> urls = Optional.ofNullable(config.getPermitUrls()).orElseThrow(RuntimeException::new).stream().map(s -> s.getUrl()).collect(Collectors.toList());
        urls.addAll(URL_PERMIT_ALL);
        for (String url: urls) {
            if(antPathMatcher.match(url,exchange.getRequest().getPath().value())) return chain.filter(exchange);
        }
        //protect url
        if(config.getWhiteListPermits().stream().anyMatch(s -> antPathMatcher.match(s.getUrl(), exchange.getRequest().getPath().value()))) return chain.filter(exchange);
        //jwt check
        return Mono.just(exchange).flatMap(r -> converter.apply(r)).switchIfEmpty(chain.filter(exchange).then(Mono.empty())).flatMap(i ->authenticate(exchange,chain,i));
    }

    /**
     * A dummy authentication manager providing standard step authentication, but
     * exchange correctness will be performed in the filtering step.
     * This is because JWT mechanism contains all required information in the token itself
     * this is called stateless session or client-side session.
     *
     * @param exchange Current WebExchange
     * @param chain Parent chain to pass successful authenticated exchanges
     * @param token The current authentication object
     *
     * @return Void when authentication is complete
     */
    private Mono<Void> authenticate(ServerWebExchange exchange,WebFilterChain chain, Authentication token) {
        return authenticationManager.authenticate(token).flatMap(s -> onAuthenticationSuccess(s,  new WebFilterExchange(exchange, chain)));
    }

    /**
     * The current exchange will be passed trough the chain on successful authentication
     * Spring security will have all needed information to authorize our current exchange
     *
     * @param authentication The current authentication object
     * @param webFilterExchange Current authentication chain
     * @return Void when completing handler
     */
    private Mono<Void> onAuthenticationSuccess(Authentication authentication, WebFilterExchange webFilterExchange) {
        SecurityContextImpl securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(authentication);
        return securityContextRepository.save(webFilterExchange.getExchange(), securityContext)
                .then(authenticationSuccessHandler.onAuthenticationSuccess(webFilterExchange, authentication))
                .subscriberContext(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
    }
}
