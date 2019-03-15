package com.xiehua.bus.disruptor;

import com.lmax.disruptor.*;
import com.xiehua.bus.Bus;
import com.xiehua.bus.Msghandler;
import com.xiehua.support.wrap.dto.ReqDTO;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SeriesDataEventQueueHelper extends BaseQueueHelper<ReqDTO, SeriesDataEvent, SeriesDataEventHandler> implements InitializingBean {

    private static final int QUEUE_SIZE = 1024 * 1024;

    @Autowired
    private List<SeriesDataEventHandler> seriesDataEventHandler;

    @Autowired
    private Msghandler msghandler;


    @Override
    protected int getQueueSize() {
        return QUEUE_SIZE;
    }

    @Override
    protected EventFactory eventFactory() {
        return new EventFactory();
    }

    @Override
    protected WorkHandler[] getHandler() {
        int size = seriesDataEventHandler.size();
        SeriesDataEventHandler[] paramEventHandlers = (SeriesDataEventHandler[]) seriesDataEventHandler.toArray(new SeriesDataEventHandler[size]);
        return paramEventHandlers;
    }

    @Override
    protected WaitStrategy getStrategy() {
//        return new BlockingWaitStrategy();
        return new SleepingWaitStrategy();
    }

    @Override
    public void afterPropertiesSet() {
        this.init();
        Bus.register(msghandler);
    }
}