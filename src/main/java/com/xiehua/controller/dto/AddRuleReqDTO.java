package com.xiehua.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class AddRuleReqDTO implements Serializable{

    @NotBlank(message = "服务名字不能为空")
    private String service;

    @NotBlank(message = "客户端配置项不能为空")
    private String key;

    @NotBlank(message = "服务端配置项不能为空")
    private String value;
}
