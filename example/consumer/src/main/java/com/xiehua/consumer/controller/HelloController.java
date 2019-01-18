package com.xiehua.consumer.controller;

import com.xiehua.consumer.feign.HelloFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Random;


@Slf4j
@RequestMapping("/")
@RestController
public class HelloController {

    @Autowired
    HelloFeignService helloRemote;

    @GetMapping("/protected/{name}")
    public String t_protected(@PathVariable("name") String name){
        return "hello"+name+",我是protected的输出"+ LocalDateTime.now().toString();
    }

    @GetMapping("/public/{name}")
    public String t_public(@PathVariable("name") String name){
        return "hello"+name+",我是public的输出"+ LocalDateTime.now().toString();
    }

    @GetMapping("/private_sleep/{name}")
    public String index(@PathVariable("name") String name) throws InterruptedException {
        //  nextInt(MAX - MIN + 1) + MIN;
        //int sleep = new Random().nextInt(1000 - 100 + 1) + 100;
        //log.info("休息" +sleep + "ms");
       //  Thread.sleep(sleep);
        log.info("我执行了:" + LocalDateTime.now());
        return "hello"+name+",我是private_sleep的输出"+ LocalDateTime.now().toString();
    }


    @GetMapping("/public_remote/{name}")
    public String helloRemote(@PathVariable("name") String name, HttpServletRequest request){
        return helloRemote.hello(name);
    }

}
