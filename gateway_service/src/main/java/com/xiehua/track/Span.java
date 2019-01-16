package com.xiehua.track;

import com.xiehua.track.tree.Node;
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

    private String serviceName;//服务名字

    private String url;//请求url

    private LocalDateTime reqTime = LocalDateTime.now();//请求时间

    private List<Span> childs;//子节点

    public Span(String traceId){
        this.traceId = traceId;
    }

    public void addChild(Span span) {
        if (this.childs == null) {
            synchronized (Span.class) {
                this.childs = new ArrayList<>();
            }
        }
        this.childs.add(span);
    }

    public static void main(String[] args) {
        Span root = new Span("root");
        Span c1 = new Span("c1");
        root.addChild(c1);
        System.out.println(root);
    }

}
