package com.xiehua.mvc.controller.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PermissionsSystem implements Serializable{

    private String system;

    private List<String> permissions;
}
