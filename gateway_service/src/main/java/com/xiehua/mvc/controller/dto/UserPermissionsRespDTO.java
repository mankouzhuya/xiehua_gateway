package com.xiehua.mvc.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPermissionsRespDTO implements Serializable{

    private String account;

    private List<PermissionsSystem> permissionsSystemList;




}
