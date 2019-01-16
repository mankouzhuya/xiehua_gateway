package com.xiehua.demo.demo3;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;

public class OrderHandlerD implements EventHandler<Order>, WorkHandler<Order> {

    @Override
    public void onEvent(Order event, long sequence, boolean endOfBatch) throws Exception {
        System.out.println("我是hander->D:"+event.getNum() + "===="+ event.getPrice());
    }

    @Override
    public void onEvent(Order event) throws Exception {
        System.out.println("我是hander->D-->WorkHandler:"+event.getNum() + "===="+ event.getPrice());
    }
}
