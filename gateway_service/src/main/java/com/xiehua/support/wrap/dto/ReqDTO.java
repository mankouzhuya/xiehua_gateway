package com.xiehua.support.wrap.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ReqDTO implements Serializable{

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
