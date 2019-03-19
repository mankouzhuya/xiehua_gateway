package com.xiehua;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerClientAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication(exclude = GatewayLoadBalancerClientAutoConfiguration.class)
@EnableDiscoveryClient
@EnableCircuitBreaker
@EnableScheduling
public class XiehuaGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiehuaGatewayApplication.class, args);
    }

}
