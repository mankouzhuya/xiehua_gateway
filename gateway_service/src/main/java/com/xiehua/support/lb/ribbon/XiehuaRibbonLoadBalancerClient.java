package com.xiehua.support.lb.ribbon;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.ribbon.*;

public class XiehuaRibbonLoadBalancerClient extends RibbonLoadBalancerClient {

    private SpringClientFactory clientFactory;

    public XiehuaRibbonLoadBalancerClient(SpringClientFactory clientFactory) {
        super(clientFactory);
        this.clientFactory = clientFactory;
    }

    public ServiceInstance choose(String serviceId,Object key) {
        Server server = getServer(serviceId,key);
        if (server == null)  return null;
        return new RibbonServer(serviceId, server, isSecure(server, serviceId), serverIntrospector(serviceId).getMetadata(server));
    }


    private ServerIntrospector serverIntrospector(String serviceId) {
        ServerIntrospector serverIntrospector = this.clientFactory.getInstance(serviceId,ServerIntrospector.class);
        if (serverIntrospector == null)  serverIntrospector = new DefaultServerIntrospector();
        return serverIntrospector;
    }

    private boolean isSecure(Server server, String serviceId) {
        IClientConfig config = this.clientFactory.getClientConfig(serviceId);
        ServerIntrospector serverIntrospector = serverIntrospector(serviceId);
        return RibbonUtils.isSecure(config, serverIntrospector, server);
    }

    protected Server getServer(String serviceId,Object key) {
        return getServer(getLoadBalancer(serviceId),key);
    }

    protected Server getServer(ILoadBalancer loadBalancer,Object key) {
        if (loadBalancer == null) return null;
        return loadBalancer.chooseServer(key);
    }


}
