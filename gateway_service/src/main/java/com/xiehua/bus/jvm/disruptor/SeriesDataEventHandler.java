package com.xiehua.bus.jvm.disruptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lmax.disruptor.WorkHandler;
import com.xiehua.bus.jvm.Msghandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

@Slf4j
public class SeriesDataEventHandler implements WorkHandler<SeriesDataEvent> {

    private Msghandler msghandler;

    private SeriesDataEventHandler() {
    }

    ;

    public SeriesDataEventHandler(Msghandler msghandler) {
        this.msghandler = msghandler;
    }

    @Override
    public void onEvent(SeriesDataEvent event) throws JsonProcessingException, ExecutionException, InterruptedException {
        if (event.getValue() == null) {
            log.warn("receiver series data is empty!");
            return;
        }
        //业务处理 deviceInfoService.processData(event.getValue().getDeviceInfoStr());
        if (log.isDebugEnabled()) log.info("处理消息:{}", event.getValue().toString());
        msghandler.process(event.getValue());

    }


}