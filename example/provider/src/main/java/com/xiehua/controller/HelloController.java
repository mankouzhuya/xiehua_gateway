package com.xiehua.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * @author: xujin
 **/
@RequestMapping("/hello")
@RestController
public class HelloController {


    @GetMapping("")
    public String hello(@RequestParam(value = "name") String name){
        return "name->"+name+",date:"+ LocalDateTime.now().toString();
    }

}
