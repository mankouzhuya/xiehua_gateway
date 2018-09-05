package com.xiehua.controller;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.xiehua.config.dto.CustomConfig;
import com.xiehua.controller.dto.AddRuleReqDTO;
import com.xiehua.controller.dto.DeleteRuleReqDTO;
import com.xiehua.controller.dto.LoginReqDTO;
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


}
