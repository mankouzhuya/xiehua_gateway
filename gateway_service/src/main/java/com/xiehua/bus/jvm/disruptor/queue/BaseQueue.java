package com.xiehua.bus.jvm.disruptor.queue;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadFactory;

@Slf4j
public abstract class BaseQueue<D, E extends ValueWrapper<D>, H extends WorkHandler<E>> {

    private Disruptor<E> disruptor;//Disruptor 对象

    private RingBuffer<E> ringBuffer;//RingBuffer

    /**
     * 队列大小
     *
     * @return 队列长度，必须是2的幂
     */
    protected abstract int getQueueSize();

    /**
     * 事件工厂
     *
     * @return EventFactory
     */
    protected abstract EventFactory eventFactory();

    /**
     * 如果要改变线程执行优先级，override此策略. YieldingWaitStrategy会提高响应并在闲时占用70%以上CPU，
     * 慎用SleepingWaitStrategy会降低响应更减少CPU占用，用于日志等场景.
     *
     * @return WaitStrategy
     */
    protected abstract WaitStrategy getStrategy();

    /**
     * 事件消费者
     *
     * @return WorkHandler[]
     */
    protected abstract WorkHandler[] getHandler();

    /**
     * 初始化
     */
    protected synchronized void init() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("DisruptorThreadPool").build();
        disruptor = new Disruptor(eventFactory(), getQueueSize(), namedThreadFactory, ProducerType.SINGLE, getStrategy());
        disruptor.setDefaultExceptionHandler(new DefaultHandlerException());
        disruptor.handleEventsWithWorkerPool(getHandler());
        ringBuffer = disruptor.start();

        //加入资源清理钩子
        Runtime.getRuntime().addShutdownHook(new Thread(()-> shutdown()));
    }



    /**
     * 发布事件
     */
    public void publishEvent(D data) {
        if(data == null) {
            log.warn("发布的事件为空");
            return ;
        }
        ringBuffer.publishEvent((m,n,t) -> m.setValue(t),data);
    }

    /**
     * 关闭队列
     */
    public void shutdown() {
        disruptor.halt();
        disruptor.shutdown();
    }
}