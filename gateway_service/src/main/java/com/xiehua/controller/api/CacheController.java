package com.xiehua.controller.api;


import com.xiehua.cache.SimpleCache;
import com.xiehua.cache.dto.SimpleKvDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/***
 * 网关内部服务接口(本地缓存查询接口)
 * **/
@RestController
@RequestMapping("/gateway/api")
public class CacheController {

    @Autowired
    private SimpleCache cache;

    /***
     * 遍历所有缓存
     * **/
    @GetMapping("/caches")
    public List<SimpleKvDTO> getAll(){
        return cache.getAll();
    }

}
