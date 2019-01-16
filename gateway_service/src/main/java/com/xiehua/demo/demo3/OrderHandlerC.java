package com.xiehua.demo.demo3;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;

public class OrderHandlerC implements EventHandler<Order> ,WorkHandler<Order> {
    /**
     * Called when a publisher has published an event to the {@link RingBuffer}.  The {@link BatchEventProcessor} will
     * read messages from the {@link RingBuffer} in batches, where a batch is all of the events available to be
     * processed without having to wait for any new event to arrive.  This can be useful for event handlers that need
     * to do slower operations like I/O as they can group together the data from multiple events into a single
     * operation.  Implementations should ensure that the operation is always performed when endOfBatch is true as
     * the time between that message an the next one is inderminate.
     *
     * @param event      published to the {@link RingBuffer}
     * @param sequence   of the event being processed
     * @param endOfBatch flag to indicate if this is the last event in a batch from the {@link RingBuffer}
     * @throws Exception if the EventHandler would like the exception handled further up the chain.
     */
    @Override
    public void onEvent(Order event, long sequence, boolean endOfBatch) throws Exception {
        System.out.println("我是hander->C-->EventHandler:"+event.getNum() + "===="+ event.getPrice());
    }

    /**
     * Callback to indicate a unit of work needs to be processed.
     *
     * @param event published to the {@link RingBuffer}
     * @throws Exception if the {@link WorkHandler} would like the exception handled further up the chain.
     */
    @Override
    public void onEvent(Order event) throws Exception {
        System.out.println("我是hander->C-->WorkHandler:"+event.getNum() + "===="+ event.getPrice());
    }
}
