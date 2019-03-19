package com.xiehua.bus.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.bus.redis.dto.XiehuaMessage;
import com.xiehua.cache.SimpleCache;
import com.xiehua.mvc.controller.dto.BroadcastRulesDTO;
import com.xiehua.mvc.controller.dto.BroadcastUserPermissionsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;

import static com.xiehua.bus.redis.dto.XiehuaMessage.*;
import static com.xiehua.config.secruity.jwt.JWTReactiveAuthenticationManager.REDIS_GATEWAY_USER_POWER_PREFIX;
import static com.xiehua.filter.RouteFilter.REDIS_GATEWAY_SERVICE_RULE;

public class XiehuaRedisListener implements MessageListener {

    private static final Logger log =  LoggerFactory.getLogger(XiehuaRedisListener.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SimpleCache defaultCache;

    @Autowired
    private ObjectMapper mapper;


    @Override
    public void onMessage(Message message, byte[] pattern) {
        Object channel = stringRedisTemplate.getValueSerializer().deserialize(message.getChannel());
        Object value = stringRedisTemplate.getValueSerializer().deserialize(message.getBody());
        String channelStr = String.valueOf(channel);
        String messageStr = String.valueOf(value);
        log.info("收到消息topic->" + channelStr + ",body-> " + messageStr + ",pattern->" + new String(pattern));
        try {
            XiehuaMessage xiehuaMessage = mapper.readValue(messageStr, XiehuaMessage.class);
            if (TYPE_CLEAR_ALL.equals(xiehuaMessage.getType())) clearLocalCache(xiehuaMessage.getContent());//清空所有缓存
            if (TYPE_UPDATE_USER_INFO.equals(xiehuaMessage.getType())) updateUserInfo(xiehuaMessage.getContent());//更新用户信息
            if (TYPE_DELETE_USER_INFO.equals(xiehuaMessage.getType())) deleteUserInfo(xiehuaMessage.getContent()); //删除用户信息
            if (TYPE_UPDATE_RULE.equals(xiehuaMessage.getType())) updateRouteInfo(xiehuaMessage.getContent()); //更新路由信息
            if (TYPE_DELETE_RULE.equals(xiehuaMessage.getType())) deleteRouteInfo(xiehuaMessage.getContent()); //删除路由信息
        } catch (IOException e) {
            log.error("序列化失败:{}",e);
        }


    }

    /**
     * 更新用户信息,此时content是AddPermissionsReqDTO的实例
     * **/
    private void updateUserInfo(String content) throws IOException {
        BroadcastUserPermissionsDTO broadcastUserPermissionsDTO = mapper.readValue(content,BroadcastUserPermissionsDTO.class);
        String key = defaultCache.genKey(REDIS_GATEWAY_USER_POWER_PREFIX + broadcastUserPermissionsDTO.getGid());
        defaultCache.put(key,broadcastUserPermissionsDTO.getUserPermissions());
    }

    /***
     * 删除用户信息,此时content是gid
     * */
    private void deleteUserInfo(String content){
        String key = defaultCache.genKey(REDIS_GATEWAY_USER_POWER_PREFIX + content);
        if(!defaultCache.exist(key)) return;
        defaultCache.remove(key);
    }

    /***
     * 更新路由信息,此时AddRule2ReqDTO的实例
     * **/
    private void updateRouteInfo(String content) throws IOException {
        BroadcastRulesDTO broadcastRulesDTO = mapper.readValue(content, BroadcastRulesDTO.class);
        String key = defaultCache.genKey(REDIS_GATEWAY_SERVICE_RULE + broadcastRulesDTO.getService());
        defaultCache.put(key,broadcastRulesDTO.getRules());
    }

    /***
     * 删除路由信息,此时content是service_name
     * **/
    private void deleteRouteInfo(String content){
        String key = defaultCache.genKey(REDIS_GATEWAY_SERVICE_RULE + content);
        if(!defaultCache.exist(key)) return;
        defaultCache.remove(key);
    }

    /**
     * 清空所有缓存
    **/
    private void clearLocalCache(String content){
        defaultCache.removeAll();
    }

}
