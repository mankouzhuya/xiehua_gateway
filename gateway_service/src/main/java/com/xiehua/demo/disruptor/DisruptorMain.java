package com.xiehua.demo.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DisruptorMain {
    public static void main(String[] args)  throws Exception{
        DisruptorFactory factory = new DisruptorFactory();          // 工厂
        ExecutorService executor = Executors.newCachedThreadPool(); // 线程池
        int BUFFER_SIZE = 16;   // 必须为2的幂指数

        // 初始化Disruptor
        Disruptor<FileData> disruptor = new Disruptor<>(factory,
                BUFFER_SIZE,
                executor,
                ProducerType.MULTI,         // Create a RingBuffer supporting multiple event publishers to the one RingBuffer
                new BlockingWaitStrategy()  // 默认阻塞策略
        );
        // 启动消费者
        disruptor.handleEventsWithWorkerPool(new DisruptorConsumer(),
                new DisruptorConsumer()
        );
        disruptor.start();
        // 启动生产者
        RingBuffer<FileData> ringBuffer = disruptor.getRingBuffer();
        DisruptorProducer producer = new DisruptorProducer(ringBuffer);
        producer.read("D:\\apache\\apache-jmeter-4.0\\README.md");
        Thread.sleep(1000);
        // 关闭
        disruptor.shutdown();
        executor.shutdown();
    }
}