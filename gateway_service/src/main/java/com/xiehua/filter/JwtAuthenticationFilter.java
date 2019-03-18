package com.xiehua.filter;

import com.xiehua.component.GateWayComponent;
import com.xiehua.config.dto.CustomConfig;
import com.xiehua.config.secruity.jwt.XiehuaAuthenticationToken;
import com.xiehua.config.secruity.jwt.converter.ServerHttpBearerAuthenticationConverter;
import com.xiehua.exception.BizException;
import io.jsonwebtoken.impl.DefaultClaims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static com.xiehua.config.dto.white_list.WhiteListPermit.*;


@Slf4j
public class JwtAuthenticationFilter extends Authenticcation implements WebFilter {

    private GateWayComponent gateWayComponent;

    private ServerHttpBearerAuthenticationConverter converter;

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(CustomConfig config, GateWayComponent gateWayComponent,ServerHttpBearerAuthenticationConverter converter, ReactiveAuthenticationManager authenticationManager, ServerSecurityContextRepository securityContextRepository, ServerAuthenticationSuccessHandler authenticationSuccessHandler) {
        super(config, authenticationManager, securityContextRepository, authenticationSuccessHandler);
        this.config = config;
        this.gateWayComponent = gateWayComponent;
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
        //OPTIONS 请求不处理
        if (exchange.getRequest().getMethod().equals(HttpMethod.OPTIONS)) return exchange.getResponse().setComplete();

        //server_id
        String path = exchange.getRequest().getURI().getPath();
        if (StringUtils.isEmpty(path)) throw new BizException("访问路径不合法:{}", path);
        String[] serverIds = path.split("/");
        if (serverIds == null || serverIds.length < 1) throw new BizException("访问的 service id 不存在");
        String serviceName = serverIds[1];
        if(serviceName.contains("_")) serviceName = serviceName.replace("_","-");
        exchange.getAttributes().put(GATEWAY_ATTR_SERVER_NAME,serviceName.toUpperCase());

        //permit all url
        ServerWebExchange webExchangeDecorator = gateWayComponent.mutateWebExchange(exchange);
        if (checkPermitUrls(webExchangeDecorator)) return chain.filter(webExchangeDecorator);

        //white list chekc
        return checkWhiteList(webExchangeDecorator, chain,serviceName);
    }

    //white list chekc
    private Mono<Void> checkWhiteList(ServerWebExchange exchange, WebFilterChain chain,String serviceName) {
        return Flux.fromIterable(config.getWhiteListPermits())
                .switchIfEmpty(Flux.error(new RuntimeException("配置路径为空或不存在")))
                .any(s -> antPathMatcher.match(s.getUrl(), exchange.getRequest().getPath().value()) && s.getIp().stream().filter(m -> m.equals(getIpAddr(exchange))).count() > 0)
                .flatMap(m -> {
                    if (m) {
                        DefaultClaims claims = new DefaultClaims();
                        claims.setIssuer(config.getApplicationName())
                                .setSubject(DEFAULT_WHITE_GID)//gid
                                .setAudience(DEFAULT_WHITE_ACCOUNT)//account
                                .setExpiration(Date.from(LocalDateTime.now().plusMinutes(1).atZone(ZoneId.systemDefault()).toInstant()))
                                .setNotBefore(new Date())
                                .setIssuedAt(new Date())
                                .setId(DEFAULT_WHITE_PWD);
                        return Mono.just(new XiehuaAuthenticationToken(null, DEFAULT_WHITE_GID,claims,serviceName)).flatMap(i -> authenticate(exchange, chain, i));
                    } else {
                        return converter.apply(exchange).publishOn(Schedulers.elastic()).flatMap(i -> authenticate(exchange, chain, i));
                    }
                });
    }


}
