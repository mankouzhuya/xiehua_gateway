package com.xiehua.fun;

@FunctionalInterface
public interface UncheckedFunction2<T, R> {
    R apply(T t) throws Exception;
}