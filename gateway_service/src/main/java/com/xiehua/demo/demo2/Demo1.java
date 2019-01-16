package com.xiehua.demo.demo2;


import com.lmax.disruptor.*;

import java.util.UUID;
import java.util.concurrent.*;

public class Demo1 {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Demo1 demo1 = new Demo1();
        int BUFFER_SIZE = 1024;
        int THREAD_NUMBERS = 4;
        /*
         * createSingleProducer创建一个单生产者的RingBuffer，
         * 第一个参数叫EventFactory，从名字上理解就是“事件工厂”，其实它的职责就是产生数据填充RingBuffer的区块。
         * 第二个参数是RingBuffer的大小，它必须是2的指数倍 目的是为了将求模运算转为&运算提高效率
         * 第三个参数是RingBuffer的生产都在没有可用区块的时候(可能是消费者（或者说是事件处理器） 太慢了)的等待策略
         */
        final RingBuffer<TradeTransaction> ringBuffer = RingBuffer.createSingleProducer(()-> demo1.new TradeTransaction(), BUFFER_SIZE, new YieldingWaitStrategy());
        //创建线程池
        ExecutorService executors = Executors.newFixedThreadPool(THREAD_NUMBERS);
        //创建SequenceBarrier
        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();

        //创建消息处理器
        BatchEventProcessor<TradeTransaction> transProcessor = new BatchEventProcessor<>(ringBuffer, sequenceBarrier, demo1.new TradeTransactionInDBHandler());

        //这一部的目的是让RingBuffer根据消费者的状态    如果只有一个消费者的情况可以省略
        ringBuffer.addGatingSequences(transProcessor.getSequence());

        //把消息处理器提交到线程池
        executors.submit(transProcessor);
        //如果存大多个消费者 那重复执行上面3行代码 把TradeTransactionInDBHandler换成其它消费者类

        Future<?> future = executors.submit(()->{
            long seq;
            for (int i = 0; i < 1000; i++) {
                seq = ringBuffer.next();//占个坑 --ringBuffer一个可用区块

                ringBuffer.get(seq).setPrice(Math.random() * 9999);//给这个区块放入 数据  如果此处不理解，想想RingBuffer的结构图

                ringBuffer.publish(seq);//发布这个区块的数据使handler(consumer)可见
            }
        });
        future.get();//等待生产者结束
        Thread.sleep(1000);//等上1秒，等消费都处理完成
        transProcessor.halt();//通知事件(或者说消息)处理器 可以结束了（并不是马上结束!!!）
        executors.shutdown();//终止线程
    }

    public class TradeTransactionInDBHandler implements EventHandler<TradeTransaction>,WorkHandler<TradeTransaction> {

        @Override
        public void onEvent(TradeTransaction event, long sequence,boolean endOfBatch) throws Exception {
            this.onEvent(event);
        }

        @Override
        public void onEvent(TradeTransaction event) throws Exception {
            //这里做具体的消费逻辑
            event.setId(UUID.randomUUID().toString());//简单生成下ID
            System.out.println(event.getId());
        }
    }

    //DEMO中使用的 消息全假定是一条交易
    public class TradeTransaction {
        private String id;//交易ID
        private double price;//交易金额

        public TradeTransaction() {
        }
        public TradeTransaction(String id, double price) {
            super();
            this.id = id;
            this.price = price;
        }
        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public double getPrice() {
            return price;
        }
        public void setPrice(double price) {
            this.price = price;
        }
    }
}







