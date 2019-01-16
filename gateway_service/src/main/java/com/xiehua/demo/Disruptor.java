package com.xiehua.demo;

import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.Sequencer;

import java.nio.ByteBuffer;

public class Disruptor {

    public static void main(String[] args) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        for(long l = 0; l<100; l++){
            byteBuffer.putLong(0, l);
            System.out.println(byteBuffer.getLong(0));
        }
    }
}
