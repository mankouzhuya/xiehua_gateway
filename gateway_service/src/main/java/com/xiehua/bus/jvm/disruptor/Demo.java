package com.xiehua.bus.jvm.disruptor;

import com.xiehua.bus.jvm.disruptor.method_queue.MethodNodeEventHandler;
import com.xiehua.bus.jvm.disruptor.method_queue.MethodNodeQueue;
import com.xiehua.support.wrap.dto.ReqDTO;

import java.util.ArrayList;
import java.util.List;

public class Demo {

    public static void main(String[] args) {
        MethodNodeEventHandler methodNodeEventHandler = new MethodNodeEventHandler();
        List<MethodNodeEventHandler> handlers = new ArrayList<>();
        handlers.add(methodNodeEventHandler);
        MethodNodeQueue methodNodeQueue = new MethodNodeQueue(handlers);

        Long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            ReqDTO methodNode = new ReqDTO();
            methodNode.setMethod("methodNode+"+i);
            methodNodeQueue.publishEvent(methodNode);
        }
        System.out.println(System.currentTimeMillis() - start);
        methodNodeQueue.shutdown();
    }
}
