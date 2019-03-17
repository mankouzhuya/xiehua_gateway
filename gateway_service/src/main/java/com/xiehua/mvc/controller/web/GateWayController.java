package com.xiehua.mvc.controller.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xiehua.mvc.controller.dto.*;
import com.xiehua.mvc.service.GateWayService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Arrays;

/***
 * 网关web api
 * **/
@Controller
@RequestMapping("/xiehua_gateway/service")
public class GateWayController {

    @Autowired
    private GateWayService gateWayService;


    /***
     * 首页
     * **/
    @GetMapping("/index")
    public Mono<String> login(final Model model, @CookieValue(HttpHeaders.AUTHORIZATION) String authentication) {
        model.addAttribute(gateWayService.index(authentication));
        return Mono.create(monoSink -> monoSink.success("index"));
    }

    /***
     * 添加或者更新路由
     * **/
    @PostMapping("/rule")
    @ResponseBody
    public Mono<Boolean> addRule(@Validated @RequestBody AddRuleReqDTO addRuleReqDTO) {
        return gateWayService.addRule(addRuleReqDTO);
    }

    /**
     * 删除路由的一条配置
     **/
    @PostMapping("/rule_d")
    @ResponseBody
    public Mono<Long> deleteRule(@Validated @RequestBody DeleteRuleReqDTO deleteRuleReqDTO) {
        return gateWayService.deleteRule(deleteRuleReqDTO);
    }

    /***
     * 删除整个路由信息
     * **/
    @DeleteMapping("/rule_d")
    @ResponseBody
    public Mono<Long> deleteRule(@RequestParam String serviceId) {
        return gateWayService.deleteRule(serviceId);
    }

    /**
     * 清空本地缓存
     **/
    @PostMapping("/clear_local_cache")
    @ResponseBody
    public void clearLocalCache() throws JsonProcessingException {
        gateWayService.clearLocalCache();
    }

    /***
     * 查询某个用户是否在线
     * **/
    @GetMapping("/is_onLine")
    @ResponseBody
    public Mono<Boolean> isOnLine(@RequestParam String account) {
        return gateWayService.isOnLine(account);
    }

    /***
     * 统计在线用户数量
     * **/
    @GetMapping("/online_count")
    @ResponseBody
    public Mono<Long> getOnLinePersion() {
        return gateWayService.getOnLinePersion();
    }

    /**
     * 添加用户权限(存在即更新,全量替换)
     * 注意:次接口是全量替换用户对应系统的权限标识,故调用的时候一定要传用户在对应系统的全部权限标识
     **/
    @GetMapping("/user_permissions")
    @ResponseBody
    public Mono<UserPermissionsRespDTO> userPermissions(@RequestParam String account) {
        return gateWayService.getUserPermissionsList(account);
    }

    /**
     * 添加用户权限(存在即更新,全量替换)
     * 注意:次接口是全量替换用户对应系统的权限标识,故调用的时候一定要传用户在对应系统的全部权限标识
     **/
    @PostMapping("/user_permissions")
    @ResponseBody
    public Mono<Void> userPermissions(@Validated @RequestBody AddPermissionsReq2DTO addPermissionsReqDTO) {
        AddPermissionsReqDTO dto = new AddPermissionsReqDTO();
        BeanUtils.copyProperties(addPermissionsReqDTO, dto);
        dto.setPermissions(Arrays.asList(addPermissionsReqDTO.getPermissions().split(",")));
        return gateWayService.addOrUpdatePermissions(dto);
    }

}
