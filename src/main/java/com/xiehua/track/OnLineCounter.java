package com.xiehua.track;

/***
 * 在线人数统计
 * */
public interface OnLineCounter {

    public static final String REDIS_GATEWAY_ONLINE_COUNTER_PREFIX = "gateway:online:counter";


    /**
     * 增加在线人数
     * **/
    void increase(String account);

    /**
     * 减少在线人数
     * **/
    void decrease(String account);

    /**
     * 统计在线人数
     * **/
    Integer count();

}
