package com.xiehua.fun;

@FunctionalInterface
public interface UncheckedPredicate<T> {

    boolean test(T t)throws Exception;

}