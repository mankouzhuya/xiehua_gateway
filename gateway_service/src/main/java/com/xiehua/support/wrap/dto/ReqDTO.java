package com.xiehua.support.wrap.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ReqDTO implements Serializable{

    public static final String REDIS_GATEWAY_TEMP_PREFIX = "gateway:temp:id_";

    public static final Integer REDIS_GATEWAY_TEMP_EXP = 60 * 1;//1分钟自动过期

    private String reqId;

    private String key;

    private String url;

    private String method;

    private String head;

    private String reqBody;

    private String respBody;

    private LocalDateTime reqTime;

    private LocalDateTime respTime;

    private Long executeTime;


}
