package com.xiehua.config.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 权限标识
 * **/
@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum SecurityRoleEnum {

    role_inner_protected("INNER_PROTECTED","ROLE_INNER_PROTECTED","内部受保护的接口权限"),role_gateway_admin("GATEWAY_ADMIN","ROLE_GATEWAY_ADMIN","网关登录超级用户");

    private String role;

    private String fullRole;

    private String showName;

}
