package com.xiehua.mvc.controller.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class BroadcastRulesDTO implements Serializable {

    private String service;

    private Map<String,String> rules;
}
