package com.xiehua.fun;

@FunctionalInterface
public interface UncheckedConsumer<T> {

    void accept(T t) throws Exception;

}