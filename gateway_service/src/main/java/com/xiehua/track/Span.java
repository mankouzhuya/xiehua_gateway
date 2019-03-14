package com.xiehua.track;

import com.xiehua.support.wrap.dto.ReqDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Span implements Serializable{

    private String traceId;//全局跟踪ID

    private String spanId;//单独一次请求的id

    private String url;//请求url

    private LocalDateTime reqTime = LocalDateTime.now();//请求时间

    private ReqDTO reqDTO;//请求响应详细

    private List<Span> childs;//子节点

    private Long executeTime;//执行时间




    public void addChild(Span span) {
        if (this.childs == null) {
            synchronized (Span.class) {
                this.childs = new ArrayList<>();
            }
        }
        this.childs.add(span);
    }

}
