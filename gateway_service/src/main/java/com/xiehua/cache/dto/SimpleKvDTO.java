package com.xiehua.cache.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SimpleKvDTO implements Serializable {

    private String key;

    private String value;

}
