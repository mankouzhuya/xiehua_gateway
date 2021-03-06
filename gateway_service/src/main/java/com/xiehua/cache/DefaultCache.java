package com.xiehua.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.xiehua.cache.dto.SimpleKvDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class DefaultCache implements SimpleCache {

    public static final String REDIS_GATEWAY_UPDATE_LOCALCACHE_TOPIC = "redis_gateway_update_localcache_topic";

    private static LoadingCache<String, Object> cache = CacheBuilder.newBuilder()
            .maximumSize(1000000)//最多存放1000000个数据
            .expireAfterAccess(7, TimeUnit.DAYS)//缓存7天，7天之后进行回收
            .recordStats()//开启，记录状态数据功能
            .build(new CacheLoader<String, Object>() {
                @Override
                public Object load(String key) throws Exception {
                    // TODO Auto-generated method stub
                    return null;
                }
            });


    /**
     * 获取一条数据
     **/
    @Override
    public Object get(String key) {
        return cache.getIfPresent(key);
    }

    /**
     * 获取全部数据
     **/
    @Override
    public Map<String, Object> getAll() {
        return cache.asMap();
    }

    /**
     * 添加一条数据
     **/
    @Override
    public Object put(String key, Object value) {
        cache.put(key, value);
        return value;
    }

    /**
     * 检查某个key是否存在
     **/
    @Override
    public Boolean exist(String key) {
        return Objects.isNull(cache.getIfPresent(key));
    }

    /**
     * 生成key
     **/
    @Override
    public String genKey(String... key) {
        if (key == null || key.length < 1) throw new IllegalArgumentException("参数不合法");
        String temp = StringUtils.join(Arrays.asList(key).stream().sorted((m, n) -> m.compareTo(n)).collect(Collectors.toList()), "&");
//        return DigestUtils.md5DigestAsHex(temp.getBytes(StandardCharsets.UTF_8));
        return temp;
    }

    /**
     * 生成key
     **/
    @Override
    public String genKey(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) throw new IllegalArgumentException("参数不合法");
        String temp = StringUtils.join(keys.stream().sorted((m, n) -> m.compareTo(n)).collect(Collectors.toList()), "&");
        return DigestUtils.md5DigestAsHex(temp.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 删除一个key value
     **/
    @Override
    public void remove(String key) {
        cache.invalidate(key);
    }

    /***
     * 删除所有数据
     * **/
    @Override
    public void removeAll() {
        cache.invalidateAll();
    }


}
