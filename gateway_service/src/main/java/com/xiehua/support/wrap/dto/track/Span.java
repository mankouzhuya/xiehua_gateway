package com.xiehua.support.wrap.dto.track;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xiehua.support.wrap.dto.ReqDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class Span implements Serializable{

    private String traceId;//全局跟踪ID

    private String spanId;//单独一次请求的id

    private String fromId;

    private String url;//请求url

    @JsonFormat(locale="zh", timezone="GMT+8", pattern="yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reqTime = LocalDateTime.now();//请求时间

    private ReqDTO reqDTO;//请求响应详细

    private List<Span> childs;//子节点

    private Long executeTime;//执行时间


    public Span(String spanId,String fromId){
        this.spanId = spanId;
        this.fromId = fromId;
    }


    public void addChild(Span span) {
        if (this.childs == null) {
            synchronized (Span.class) {
                this.childs = new ArrayList<>();
            }
        }
        this.childs.add(span);
    }

}
