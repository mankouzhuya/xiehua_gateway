package com.xiehua.mvc.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class AddPermissionsReq2DTO implements Serializable {

    @NotBlank(message = "用户账号不能为空")
    private String account;

    @NotBlank(message = "系统标识不能为空(系统标识为微服务注册到发现中心的名字,注意大小写,例:PAY-CENTER)")
    private String sys;

    @NotNull(message = "权限标识不能为空")
    private String permissions;
}
