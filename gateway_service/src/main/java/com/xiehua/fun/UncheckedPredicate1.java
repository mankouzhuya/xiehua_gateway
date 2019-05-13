package com.xiehua.fun;

@FunctionalInterface
public interface UncheckedPredicate1<T> {

    boolean test(T t)throws Exception;

}