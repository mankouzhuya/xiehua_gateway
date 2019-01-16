package com.xiehua.demo.demo3;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.xiehua.demo.LongEvent;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Demo3 {

    private static int BUFFER_SIZE = 1024;

    private static int THREAD_NUMBERS = 4;

    private static ExecutorService EXECUTORS = Executors.newFixedThreadPool(THREAD_NUMBERS);

    public static void main(String[] args) throws Exception{
        Disruptor<Order> disruptor = new Disruptor<Order>(new OrderFactory(), BUFFER_SIZE, Executors.defaultThreadFactory(), ProducerType.SINGLE, new YieldingWaitStrategy());

//        final RingBuffer<Order> ringBuffer = RingBuffer.createSingleProducer(new OrderFactory(), BUFFER_SIZE, new YieldingWaitStrategy());


        Demo3 demo3 = new Demo3();
       // demo3.test6(disruptor);
        Thread.sleep(1000);
        disruptor.halt();
        disruptor.shutdown();
       // EXECUTORS.shutdown();//终止线程

        demo3.test7();
    }

    //生产事件
    public Future genEvent(final RingBuffer<Order> ringBuffer){
        OrderProducer orderProducer = new OrderProducer(ringBuffer);
        for(int i = 0;i < 2;i++) orderProducer.submitOrder(LocalDateTime.now().getNano());
//        Future<?> future = EXECUTORS.submit(()->{
//            //for(int i = 0;i < 2;i++) orderProducer.submitOrder(LocalDateTime.now().getNano());
//        });
        return null;
    }




    //单生产者模式，单消费者模式
    public Future test1( Disruptor<Order> disruptor){
        disruptor.handleEventsWith( new OrderHandlerA());
        disruptor.start();
        return genEvent(disruptor.getRingBuffer());
    }

    //单生产者多消费者，多消费者间形成依赖关系，每个依赖节点只有一个消费者
    public Future test2( Disruptor<Order> disruptor){
        //多个消费者间形成依赖关系，每个依赖节点的消费者为单线程。
        disruptor.handleEventsWith( new OrderHandlerB()).then(new OrderHandlerA());
        disruptor.start();
        return genEvent(disruptor.getRingBuffer());
    }

    //单生产者，多消费者模式。多消费者对于消息不重复消费。
    public Future test3( Disruptor<Order> disruptor){
        //多个消费者间形成依赖关系，每个依赖节点的消费者为单线程。
        disruptor.handleEventsWithWorkerPool(new OrderHandlerC(),new OrderHandlerD());
        disruptor.start();
        return genEvent(disruptor.getRingBuffer());
    }

    //单生产者多消费者，多消费者对于消息m独立消费
    public Future test4( Disruptor<Order> disruptor){
        /*
         * 两个消费者创建EventHandlerGroup。该消费者需要实现EventHandler类。两个消费者对于RingBuffer中的每个消息，都独立消费一次。
         * 两个消费者在消费消息的过程中，各自独立，不产生竞争。
         */
        disruptor.handleEventsWith(new OrderHandlerB(),new OrderHandlerA(),new OrderHandlerC());
        disruptor.start();
        return genEvent(disruptor.getRingBuffer());
    }

    //单生产者，多消费者间存在依赖关系的模式。消费者1、2消息独立消费。消费者3、4仅能消费1、2均消费过的消息，消费者5仅能消费3、4均消费过的消息
    public Future test5( Disruptor<Order> disruptor){
        disruptor.handleEventsWith(new OrderHandlerA(),new OrderHandlerB()).then(new OrderHandlerE()).then(new OrderHandlerC(),new OrderHandlerD());
        disruptor.start();
        return genEvent(disruptor.getRingBuffer());
    }

    //单生产者，多消费者。多消费者之间不重复消费，且不同的消费者WorkPool之间存在依赖关系。
    public Future test6( Disruptor<Order> disruptor){
        //消费者1、2不重复消费消息，消费者3、4不重复消费1或者2消费过的消息，消费者5消费消费者3或4消费过的消息
        disruptor.handleEventsWithWorkerPool(new OrderHandlerC(),new OrderHandlerD()).handleEventsWith(new OrderHandlerA(),new OrderHandlerB());
        disruptor.start();
        return genEvent(disruptor.getRingBuffer());
    }

    //多生产者，单消费者模式
    public void test7() throws Exception{
        Disruptor<Order> disruptor = new Disruptor<Order>(new OrderFactory(), BUFFER_SIZE, Executors.defaultThreadFactory(), ProducerType.MULTI, new YieldingWaitStrategy());
        disruptor.handleEventsWith(new OrderHandlerA()).handleEventsWithWorkerPool(new OrderHandlerC(),new OrderHandlerD());
        disruptor.start();
        CountDownLatch countDownLatch = new CountDownLatch(3);
        for(int i = 0;i < 3;i++){
            EXECUTORS.submit(()->{
               for(int j = 0;j < 3;j++){
                   OrderProducer orderProducer = new OrderProducer(disruptor.getRingBuffer());
                   orderProducer.submitOrder(LocalDateTime.now().getNano());
               }
            });
            countDownLatch.countDown();
        }
        countDownLatch.await();
        Thread.sleep(1000);
        disruptor.halt();
        disruptor.shutdown();
        EXECUTORS.shutdown();
    }

    public void oneProducerToOneBatchEventProcessor(final RingBuffer<Order> ringBuffer) throws Exception{
        //创建消息处理器
        BatchEventProcessor<Order> transProcessor = new BatchEventProcessor<>(ringBuffer, ringBuffer.newBarrier(), new OrderHandlerA());
        //这一部的目的是让RingBuffer根据消费者的状态    如果只有一个消费者的情况可以省略
        ringBuffer.addGatingSequences(transProcessor.getSequence());

        //把消息处理器提交到线程池
        //EXECUTORS.submit(transProcessor);

        Demo3 demo3 = new Demo3();
        demo3.genEvent(ringBuffer).get();//等待生产者结束

        Thread.sleep(1000);//等上1秒，等消费都处理完成
        transProcessor.halt();//通知事件(或者说消息)处理器 可以结束了（并不是马上结束!!!）
    }


}
