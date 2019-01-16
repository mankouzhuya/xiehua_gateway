package com.xiehua.demo.disruptor;

import com.lmax.disruptor.EventFactory;

public class DisruptorFactory implements EventFactory<FileData> {

    public FileData newInstance() {
        return new FileData();
    }
}