package com.xiehua.mvc.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class LoginReqDTO implements Serializable{

    @NotBlank(message = "用户名不能为空")
    private String name;

    @NotBlank(message = "密码不能为空")
    private String pwd;

    @NotBlank(message = "验证码不能为空")
    private String code;

}
