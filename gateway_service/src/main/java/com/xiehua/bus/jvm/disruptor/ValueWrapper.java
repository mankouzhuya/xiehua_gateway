package com.xiehua.bus.jvm.disruptor;

public abstract class ValueWrapper<T> {

    private T value;

    public ValueWrapper() {}

    public ValueWrapper(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}