package com.xiehua.mvc.controller.api;


import com.xiehua.cache.SimpleCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.xiehua.config.AppConfig.APPLICATION_NAME;

/***
 * 网关内部服务接口(本地缓存查询接口)
 * **/
@RestController
@RequestMapping("/"+APPLICATION_NAME+"/api")
public class CacheController {

    @Autowired
    private SimpleCache cache;

    /***
     * 遍历所有缓存
     * **/
    @GetMapping("/caches")
    public Map<String, Object> getAll(){
        return cache.getAll();
    }

}
