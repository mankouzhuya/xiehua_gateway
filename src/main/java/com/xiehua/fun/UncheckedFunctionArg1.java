package com.xiehua.fun;

@FunctionalInterface
public interface UncheckedFunctionArg1<T, R> {
    R apply(T t) throws Exception;
}