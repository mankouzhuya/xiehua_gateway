package com.xiehua.authentication;

import com.xiehua.component.GateWayComponent;
import com.xiehua.config.dto.CustomConfig;
import com.xiehua.config.dto.jwt.JwtUser;
import com.xiehua.support.wrap.XiehuaServerWebExchangeDecorator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.WebFilterChainServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.xiehua.secruity.GateWaySecurity.URL_PERMIT_ALL;

@Slf4j
public class IPFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(IPFilter.class);

    private CustomConfig config;

    private GateWayComponent gateWayComponent;

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    private ServerSecurityContextRepository securityContextRepository = NoOpServerSecurityContextRepository.getInstance();

    private ServerAuthenticationSuccessHandler authenticationSuccessHandler = new WebFilterChainServerAuthenticationSuccessHandler();

    public IPFilter(CustomConfig config, GateWayComponent gateWayComponent) {
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
//        if (logger.isDebugEnabled()) logger.debug("收到请求:{}", exchange.getRequest().getURI().toString());
        //OPTIONS 请求不处理
        if (exchange.getRequest().getMethod().equals(HttpMethod.OPTIONS)) return exchange.getResponse().setComplete();
        //permit all url
        List<String> urls = Optional.ofNullable(config.getPermitUrls()).orElseThrow(RuntimeException::new).stream().map(s -> s.getUrl()).collect(Collectors.toList());
        urls.addAll(URL_PERMIT_ALL);
        XiehuaServerWebExchangeDecorator webExchangeDecorator = gateWayComponent.mutateWebExchange(exchange);
        for (String url : urls) {
            if (antPathMatcher.match(url, webExchangeDecorator.getRequest().getPath().value())) return chain.filter(webExchangeDecorator);
        }
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
                        //build default user
                        List<GrantedAuthority> list = Arrays.asList(CustomConfig.SecurityRoleEnum.role_inner_protected.getFullRole()).stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                        UserDetails userDetails = new JwtUser("xiehua_gid", "xiehua_account", "xiehua_pwd","xiehua_gateway", list);
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, "xiehua_gid", userDetails.getAuthorities());
                        SecurityContextImpl securityContext = new SecurityContextImpl();
                        securityContext.setAuthentication(authentication);
                        return securityContextRepository.save(exchange, securityContext).then(authenticationSuccessHandler.onAuthenticationSuccess(new WebFilterExchange(exchange, chain), authentication)).subscriberContext(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
                    } else {
                        return chain.filter(exchange);
                    }
                });
    }

    /**
     * get ip address from server web exchange
     **/
    public static String getIpAddr(ServerWebExchange exchange) {
        if (exchange == null) throw new NullPointerException("getIpAddr method ServerWebExchange Object is null");
        String ipString = exchange.getRequest().getHeaders().getFirst("x-forwarded-for");
        if (StringUtils.isBlank(ipString) || "unknown".equalsIgnoreCase(ipString)) {
            ipString = exchange.getRequest().getHeaders().getFirst("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ipString) || "unknown".equalsIgnoreCase(ipString)) {
            ipString = exchange.getRequest().getHeaders().getFirst("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ipString) || "unknown".equalsIgnoreCase(ipString)) {
            ipString = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        // 多个路由时，取第一个非unknown的ip
        final String[] arr = ipString.split(",");
        for (final String str : arr) {
            if (!"unknown".equalsIgnoreCase(str)) {
                ipString = str;
                break;
            }
        }
        return ipString;
    }




}
