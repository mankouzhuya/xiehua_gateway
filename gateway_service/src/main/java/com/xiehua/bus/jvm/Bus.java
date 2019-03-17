package com.xiehua.bus.jvm;


import com.google.common.eventbus.AsyncEventBus;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Bus {

    private static Executor executor = Executors.newFixedThreadPool(20);

    private static AsyncEventBus eventBus = new AsyncEventBus(executor);

    private Bus() { }

    public static AsyncEventBus getInstance() {
        return eventBus;
    }

    public static void register(Object obj) {
        eventBus.register(obj);
    }

    public static void unregister(Object obj) {
        eventBus.unregister(obj);
    }

    public static void post(Object obj) {
        eventBus.post(obj);
    }

    public static Executor getExecutor(){
        return executor;
    }

}
