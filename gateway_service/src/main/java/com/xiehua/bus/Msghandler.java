package com.xiehua.bus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import com.xiehua.support.wrap.collect.CountTool;
import com.xiehua.support.wrap.collect.TrackTool;
import com.xiehua.support.wrap.dto.ReqDTO;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

import static com.xiehua.support.wrap.dto.ReqDTO.*;

@Slf4j
public class Msghandler {

    private StatefulRedisConnection<String, String> connection;

    private ObjectMapper mapper;

    private CountTool countTool;

    private TrackTool trackTool;

    private Msghandler(){};

    public Msghandler(StatefulRedisConnection<String, String> connection,ObjectMapper mapper,CountTool countTool,TrackTool trackTool){
        this.connection = connection;
        this.mapper = mapper;
        this.countTool = countTool;
        this.trackTool = trackTool;
    }


    /**
     * 只有通过@Subscribe注解的方法才会被注册进EventBus
     * 而且方法有且只能有1个参数
     *
     * @param reqDTO
     */
    @Subscribe
    public void process( ReqDTO reqDTO) throws JsonProcessingException, ExecutionException, InterruptedException {
        if(TYPE_SAVE_TEMP.equals(reqDTO.getType())){//临时保存ReqDTO
            saveTempReqDTO(reqDTO);
            return ;
        }
        if(TYPE_COUNT_EXEC_TIME.equals(reqDTO.getType())){//计算是否超时并持久化
            countTool.countExecuteTime(reqDTO);
            return ;
        }
        if(TYPE_SAVE_TRACK.equals(reqDTO.getType())){//追踪链保存
            trackTool.track(reqDTO);
            return ;
        }
    }

    public ReqDTO saveTempReqDTO(ReqDTO reqDTO) {
        try {
            RedisAsyncCommands<String, String> commands = connection.async();
            String key = REDIS_GATEWAY_TEMP_PREFIX + reqDTO.getTrackId();
            commands.hset(key,reqDTO.getReqId(),mapper.writeValueAsString(reqDTO));
            commands.expire(key, REDIS_GATEWAY_TEMP_EXP);
        } catch (JsonProcessingException e) {
            log.error("持久化失败:{}", e);
        } finally {
            return reqDTO;
        }
    }
}
