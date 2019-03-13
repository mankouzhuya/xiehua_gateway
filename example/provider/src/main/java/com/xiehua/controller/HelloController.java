package com.xiehua.controller;

import com.xiehua.dto.UserDTO;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public UserDTO hello2(@RequestParam(value = "name") String name, @RequestBody UserDTO userDTO){
        userDTO.setHello("你好,现在是北京时间:"+ LocalDateTime.now().toString());
        return userDTO;
    }

}
