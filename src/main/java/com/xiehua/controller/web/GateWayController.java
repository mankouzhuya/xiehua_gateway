package com.xiehua.controller.web;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.xiehua.config.dto.CustomConfig;
import com.xiehua.controller.dto.*;
import com.xiehua.encrypt.aes.AESUtil;
import com.xiehua.exception.BizException;
import com.xiehua.service.GateWayService;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

import static com.xiehua.converter.ServerHttpBearerAuthenticationConverter.BEARER;

/***
 * 网关web api
 * **/
@Controller
@RequestMapping("/gateway/service")
public class GateWayController {

    @Autowired
    private GateWayService gateWayService;


    @GetMapping("/index")
    public Mono<String> login(final Model model,@CookieValue(HttpHeaders.AUTHORIZATION) String authentication) {
        model.addAttribute(gateWayService.index(authentication));
        return Mono.create(monoSink -> monoSink.success("index"));
    }

    @PostMapping("/rule")
    @ResponseBody
    public Mono<Boolean> addRule(@Validated @RequestBody AddRuleReqDTO addRuleReqDTO) {
        return gateWayService.addRule(addRuleReqDTO);
    }

    @PostMapping("/rule_d")
    @ResponseBody
    public Mono<Long> deleteRule(@Validated @RequestBody DeleteRuleReqDTO deleteRuleReqDTO) {
        return gateWayService.deleteRule(deleteRuleReqDTO);
    }

    @GetMapping("/is_onLine")
    @ResponseBody
    public Mono<Boolean> isOnLine(@RequestParam String account){
        return gateWayService.isOnLine(account);
    }

    @GetMapping("/online_count")
    @ResponseBody
    public Mono<Long> getOnLinePersion(){
        return gateWayService.getOnLinePersion();
    }

    /**
     * 添加用户权限(存在即更新,全量替换)
     * 注意:次接口是全量替换用户对应系统的权限标识,故调用的时候一定要传用户在对应系统的全部权限标识
     **/
    @GetMapping("/user_permissions")
    @ResponseBody
    public Mono<UserPermissionsRespDTO> userPermissions(@RequestParam String account){
        return gateWayService.getUserPermissionsList(account);
    }

    /**
     * 添加用户权限(存在即更新,全量替换)
     * 注意:次接口是全量替换用户对应系统的权限标识,故调用的时候一定要传用户在对应系统的全部权限标识
     **/
    @PostMapping("/user_permissions")
    public Mono<Void> userPermissions(@Validated @RequestBody AddPermissionsReq2DTO addPermissionsReqDTO){
        return gateWayService.addPermissions(addPermissionsReqDTO);
    }

}
