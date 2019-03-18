package com.xiehua.mvc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.xiehua.config.AppConfig.APPLICATION_NAME;

/***
 * 服务降级时返回数据
 * **/
@RestController
@RequestMapping("/"+APPLICATION_NAME+"/fallback")
public class FallbackController {

    @GetMapping("")
    public String fallback() {
        return "服务器君正在玩命加载中,请稍后重试...";
    }

}