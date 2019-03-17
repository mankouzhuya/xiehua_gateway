package com.xiehua.config.enums;

import lombok.AllArgsConstructor;

/**
 * 客户端支持的字段
 * **/
@AllArgsConstructor
public enum ClientField {

    version("版本", "version");

    private String name;

    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}