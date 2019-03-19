package com.xiehua.cache;

import com.xiehua.cache.dto.SimpleKvDTO;

import java.util.List;
import java.util.Map;

public interface SimpleCache {

    /**
     * 获取一条数据
     **/
    Object get(String key);

    /**
     * 获取全部数据
     **/
    Map<String, Object> getAll();

    /**
     * 添加一条数据
     **/
    Object put(String key, Object value);


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
