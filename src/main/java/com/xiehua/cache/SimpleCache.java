package com.xiehua.cache;

import com.xiehua.cache.dto.SimpleKvDTO;

import java.util.List;

public interface SimpleCache {

    /**
     * 获取一条数据
     **/
    String get(String key);

    /**
     * 获取全部数据
     **/
    List<SimpleKvDTO> getAll();

    /**
     * 添加一条数据
     **/
    String put(String key, String value);


    /**
     * 检查某个key是否存在
     **/
    Boolean exist(String key);

    /**
     * 生成key
     **/
    String genKey(String... key);

    /**
     * 生成key
     **/
    String genKey(List<String> keys);

    /**
     * 删除一个key value
     **/
    void remove(String key);


    /***
     * 删除所有数据
     * **/
    void removeAll();


}
