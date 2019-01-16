package com.xiehua.demo.disruptor;

import com.lmax.disruptor.RingBuffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class DisruptorProducer {
    private static final String FINIDHED = "EOF";
    private final RingBuffer<FileData> ringBuffer;

    public DisruptorProducer(RingBuffer<FileData> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void pushData(String line) {
        long seq = ringBuffer.next();
        try {
            FileData event = ringBuffer.get(seq);   // 获取可用位置
            event.setLine(line);                    // 填充可用位置
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ringBuffer.publish(seq);        // 通知消费者
        }
    }

    public void read(String fileName) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
            String line;
            while ((line = reader.readLine()) != null) {
                // 生产数据
                pushData(line);
            }
            // 结束标志
            pushData(FINIDHED);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}