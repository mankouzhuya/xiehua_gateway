package com.xiehua.controller.api;

import com.xiehua.controller.dto.AddPermissionsReqDTO;
import com.xiehua.controller.dto.UserPermissionsRespDTO;
import com.xiehua.service.GateWayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/***
 * 网关内部服务接口(用户接口)
 * **/
@RestController
@RequestMapping("/gateway/api")
public class UserController {

    @Autowired
    private GateWayService gateWayService;


    /**
     * 查询用户权限
     **/
    @GetMapping("/user/permissions")
    public Mono<UserPermissionsRespDTO> getUserPermissionsList(@RequestParam String account) {
        return gateWayService.getUserPermissionsList(account);
    }

    /**
     * 添加用户权限(存在即更新,全量替换)
     * 注意:次接口是全量替换用户对应系统的权限标识,故调用的时候一定要传用户在对应系统的全部权限标识
     **/
    @PostMapping("/user/permissions")
    public Mono<Void> addPermissions(@Validated @RequestBody AddPermissionsReqDTO addPermissionsReqDTO){
        return gateWayService.addPermissions(addPermissionsReqDTO);
    }
}
