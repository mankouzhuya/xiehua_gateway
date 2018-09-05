package com.xiehua.track;

import lombok.Data;

import java.io.Serializable;

@Data
public class Span implements Serializable{

    private String traceId;//全局跟踪ID

    private String id;//span的id

}
