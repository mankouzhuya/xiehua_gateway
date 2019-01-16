package com.xiehua.controller.api;

import com.xiehua.config.dto.jwt.JwtUser;
import com.xiehua.controller.dto.UserPermissionsRespDTO;
import com.xiehua.service.GateWayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/***
 * 网关内部服务接口(登录信息查询接口)
 * **/
@RestController
@RequestMapping("/gateway/api")
public class LoginInfoController {

    @Autowired
    private GateWayService gateWayService;

    /**
     * 获取当前登录用户信息
     **/
    @GetMapping("/login_info/current_user")
    public Mono<JwtUser> getCurrentLoginUser(@RequestParam String tid) {
        return gateWayService.getCurrentLoginUser(tid);
    }

    /***
     * 检查用户是否在线
     * **/
    @GetMapping("/login_info/is_onLine")
    public Mono<Boolean> isOnLine(@RequestParam String account) {
        return gateWayService.isOnLine(account);
    }

    /***
     * 统计所有在线用户
     * **/
    @GetMapping("/login_info/online_count")
    public Mono<Long> getOnLinePersion() {
        return gateWayService.getOnLinePersion();
    }

}
