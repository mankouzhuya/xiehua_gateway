package com.xiehua.consumer.feign;

import com.xiehua.consumer.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@Slf4j
@Component
public class HelloFeignProviderHystrix implements HelloFeignService {

    @Override
    public String hello(@RequestParam(value = "name") String name){
        return "Hello World!";
    }

    @Override
    public UserDTO hello2(@RequestParam(value = "name") String name, @RequestBody UserDTO userDTO) {
        log.error("出错了");
        return null;
    }

}