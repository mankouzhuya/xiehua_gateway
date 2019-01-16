package com.xiehua.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class DeleteRuleReqDTO implements Serializable{

    @NotBlank(message = "服务名字不能为空")
    private String service;

    @NotBlank(message = "key不能为空")
    private String key;

}
