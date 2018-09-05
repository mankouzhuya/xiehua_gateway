package com.xiehua.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {

    @GetMapping("/fallback")
    public String fallback() {
        return "服务器提了一个问题，我们正在紧张地撰写答案...";
    }

}