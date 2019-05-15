package com.xiehua.bus.jvm.disruptor.method_queue;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class MethodNodeEventHandler implements EventHandler<MethodNodeEvent>,WorkHandler<MethodNodeEvent> {

    public MethodNodeEventHandler() {

    }

    @Override
    public void onEvent(MethodNodeEvent event){
        if (event.getValue() == null) {
            log.warn("receiver series data is empty!");
            return;
        }
        //业务处理 deviceInfoService.processData(event.getValue().getDeviceInfoStr());
        log.info("处理消息:{}", event.getValue().toString());
    }


    @Override
    public void onEvent(MethodNodeEvent methodNodeEvent, long l, boolean b) throws Exception {
        if(log.isDebugEnabled()) log.debug("接受到消息:" + methodNodeEvent.toString());
        this.onEvent(methodNodeEvent);
    }
}