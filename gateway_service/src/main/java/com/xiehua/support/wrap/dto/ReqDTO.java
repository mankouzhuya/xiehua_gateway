package com.xiehua.support.wrap.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ReqDTO implements Serializable{

    public static final String REDIS_GATEWAY_TEMP_PREFIX = "gateway:temp:id_";

    public static final Integer REDIS_GATEWAY_TEMP_EXP = 60 * 1;//1分钟自动过期

    public static Integer TYPE_COUNT_EXEC_TIME = 0;//计算执行时间

    public static Integer TYPE_SAVE_TEMP = 10;//零时保存

    public static Integer TYPE_SAVE_TRACK = 11;//序列保存

    private String trackId;

    private String reqId;

    private String fromId;

    private String url;

    private String method;

    private Map<String,String> reqhead;

    private String reqBody;

    private Map<String,String> resphead;

    private String respBody;

    private LocalDateTime reqTime;

    private LocalDateTime respTime;

    private Long executeTime;

    @JsonIgnore
    private Integer type;

    @JsonIgnore
    private Map<String,Object> bizMap;


}
