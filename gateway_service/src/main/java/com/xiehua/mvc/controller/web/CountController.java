package com.xiehua.mvc.controller.web;

import com.xiehua.mvc.service.GateWayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import static com.xiehua.config.AppConfig.APPLICATION_NAME;

/**
 * 网关web登录
 **/
@Controller
@RequestMapping("/" + APPLICATION_NAME + "/count")
public class CountController {

    @Autowired
    private GateWayService gateWayService;

    @Value("${spring.application.name:xiehua_gateway}")
    private String applicationName;

    @GetMapping("/index")
    public Mono<String> login(final Model model, @CookieValue(HttpHeaders.AUTHORIZATION) String authentication) {
        model.addAttribute(gateWayService.countIndexRespDTO(authentication));
        return Mono.create(monoSink -> monoSink.success("count_index"));
    }

}
