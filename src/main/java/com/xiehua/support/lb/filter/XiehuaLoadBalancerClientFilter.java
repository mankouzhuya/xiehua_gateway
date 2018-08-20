package com.xiehua.support.lb.filter;

import com.xiehua.support.lb.ribbon.XiehuaRibbonLoadBalancerClient;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.LoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.addOriginalRequestUrl;

public class XiehuaLoadBalancerClientFilter extends LoadBalancerClientFilter {

    public static final String CLIENT_VERSION = "version";

    private XiehuaRibbonLoadBalancerClient ribbonLoadBalancerClient;

    public XiehuaLoadBalancerClientFilter(XiehuaRibbonLoadBalancerClient ribbonLoadBalancerClient) {
        super(ribbonLoadBalancerClient);
        this.ribbonLoadBalancerClient = ribbonLoadBalancerClient;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI url = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        String schemePrefix = exchange.getAttribute(GATEWAY_SCHEME_PREFIX_ATTR);
        if (url == null || (!"lb".equals(url.getScheme()) && !"lb".equals(schemePrefix))) return chain.filter(exchange);
        //preserve the original url
        addOriginalRequestUrl(exchange, url);
        //version
        String version = exchange.getRequest().getHeaders().getFirst(CLIENT_VERSION);
        Map<String,String> map = new HashMap<>();
        if(StringUtils.isEmpty(version)){
            map.put(CLIENT_VERSION,"");
        }else{
            map.put(CLIENT_VERSION,version);
        }
        final ServiceInstance instance = ribbonLoadBalancerClient.choose(url.getHost(),map);

        if (instance == null)  throw new NotFoundException("Unable to find instance for " + url.getHost());

        URI uri = exchange.getRequest().getURI();

        // if the `lb:<scheme>` mechanism was used, use `<scheme>` as the default,
        // if the loadbalancer doesn't provide one.
        String overrideScheme = null;
        if (schemePrefix != null) {
            overrideScheme = url.getScheme();
        }

        URI requestUrl = ribbonLoadBalancerClient.reconstructURI(new DelegatingServiceInstance(instance, overrideScheme), uri);

        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
        return chain.filter(exchange);
    }

    class DelegatingServiceInstance implements ServiceInstance {
        final ServiceInstance delegate;
        private String overrideScheme;

        DelegatingServiceInstance(ServiceInstance delegate, String overrideScheme) {
            this.delegate = delegate;
            this.overrideScheme = overrideScheme;
        }

        @Override
        public String getServiceId() {
            return delegate.getServiceId();
        }

        @Override
        public String getHost() {
            return delegate.getHost();
        }

        @Override
        public int getPort() {
            return delegate.getPort();
        }

        @Override
        public boolean isSecure() {
            return delegate.isSecure();
        }

        @Override
        public URI getUri() {
            return delegate.getUri();
        }

        @Override
        public Map<String, String> getMetadata() {
            return delegate.getMetadata();
        }

        @Override
        public String getScheme() {
            String scheme = delegate.getScheme();
            if (scheme != null) {
                return scheme;
            }
            return this.overrideScheme;
        }

    }

}
