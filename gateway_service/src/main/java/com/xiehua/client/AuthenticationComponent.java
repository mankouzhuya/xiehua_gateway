package com.xiehua.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AuthenticationComponent {

    private static final WebClient client = WebClient.create("http://example.org");

    /**
     * 获取当前登录用户信息
     * **/
    public void getCurrentLoginUser(String tid){

    }




}
