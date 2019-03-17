package com.xiehua.filter;

import com.xiehua.config.dto.CustomConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.xiehua.config.secruity.GateWaySecurity.URL_PERMIT_ALL;

@Slf4j
public class Authenticcation {

    public static final String GATEWAY_ATTR_SERVER_NAME = "gateway_attr_server_name";//服务名字

    private static final String SCHEME_REGEX = "[a-zA-Z]([a-zA-Z]|\\d|\\+|\\.|-)*:.*";

    private static final Pattern schemePattern = Pattern.compile(SCHEME_REGEX);

    private ReactiveAuthenticationManager authenticationManager;

    private ServerSecurityContextRepository securityContextRepository;

    private ServerAuthenticationSuccessHandler authenticationSuccessHandler;

    protected CustomConfig config;

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    private Authenticcation(){}

    public Authenticcation (CustomConfig customConfig,ReactiveAuthenticationManager authenticationManager,ServerSecurityContextRepository securityContextRepository,ServerAuthenticationSuccessHandler authenticationSuccessHandler){
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.config = customConfig;
    }


    public Mono<Void> authenticate(ServerWebExchange exchange, WebFilterChain chain, Authentication token) {
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
        SecurityContextImpl securityContext = new SecurityContextImpl(authentication);
        return securityContextRepository.save(webFilterExchange.getExchange(), securityContext)
                .then(authenticationSuccessHandler.onAuthenticationSuccess(webFilterExchange, authentication))
                .subscriberContext(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
    }

    public boolean checkPermitUrls(ServerWebExchange serverWebExchange){
        //permit all url
        List<String> permitUrls = Optional.ofNullable(config.getPermitUrls()).orElseThrow(RuntimeException::new).stream().map(s -> s.getUrl()).collect(Collectors.toList());
        permitUrls.addAll(URL_PERMIT_ALL);
        return permitUrls.stream().anyMatch(s -> antPathMatcher.match(s, serverWebExchange.getRequest().getPath().value()));
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
