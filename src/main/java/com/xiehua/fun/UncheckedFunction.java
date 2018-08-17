package com.xiehua.fun;

@FunctionalInterface
public interface UncheckedFunction<T, R> {
    R apply(T t) throws Exception;
}