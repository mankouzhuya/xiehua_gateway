package com.xiehua.filter;

import com.xiehua.component.GateWayComponent;
import com.xiehua.config.dto.CustomConfig;
import com.xiehua.config.secruity.jwt.XiehuaAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.xiehua.config.dto.white_list.WhiteListPermit.DEFAULT_WHITE_GID;
import static com.xiehua.config.secruity.GateWaySecurity.URL_PERMIT_ALL;


@Slf4j
public class IPFilter extends Authenticcation implements WebFilter {


    private CustomConfig config;

    private GateWayComponent gateWayComponent;

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    public IPFilter(CustomConfig config,GateWayComponent gateWayComponent,ReactiveAuthenticationManager authenticationManager, ServerSecurityContextRepository securityContextRepository, ServerAuthenticationSuccessHandler authenticationSuccessHandler) {
        super(authenticationManager, securityContextRepository, authenticationSuccessHandler);
        this.config = config;
        this.gateWayComponent = gateWayComponent;
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
        //OPTIONS 请求不处理
        if (exchange.getRequest().getMethod().equals(HttpMethod.OPTIONS)) return exchange.getResponse().setComplete();

        exchange.getAttributes().put(GATEWAY_ATTR_SERVER_NAME, getServiceName(exchange));

        //permit all url
        List<String> permitUrls = Optional.ofNullable(config.getPermitUrls()).orElseThrow(RuntimeException::new).stream().map(s -> s.getUrl()).collect(Collectors.toList());
        permitUrls.addAll(URL_PERMIT_ALL);
        ServerWebExchange webExchangeDecorator = gateWayComponent.mutateWebExchange(exchange);
        if (permitUrls.stream().anyMatch(s -> antPathMatcher.match(s, webExchangeDecorator.getRequest().getPath().value())))
            return chain.filter(webExchangeDecorator);
        //white list chekc
        return checkWhiteList(webExchangeDecorator, chain);
    }

    //white list chekc
    private Mono<Void> checkWhiteList(ServerWebExchange exchange, WebFilterChain chain) {
        return Flux.fromIterable(config.getWhiteListPermits())
                .switchIfEmpty(Flux.error(new RuntimeException("配置路径为空或不存在")))
                .any(s -> antPathMatcher.match(s.getUrl(), exchange.getRequest().getPath().value()) && s.getIp().stream().filter(m -> m.equals(getIpAddr(exchange))).count() > 0)
                .flatMap(m -> {
                    if (m) {
                        return Mono.just(new XiehuaAuthenticationToken(null, DEFAULT_WHITE_GID)).flatMap(i -> authenticate(exchange, chain, i));
                    } else {
                        return chain.filter(exchange);
                    }
                });
    }


}
