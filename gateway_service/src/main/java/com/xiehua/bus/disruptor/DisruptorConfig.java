package com.xiehua.bus.disruptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.bus.Msghandler;
import com.xiehua.support.wrap.collect.CountTool;
import com.xiehua.support.wrap.collect.TrackTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@Configuration
//多实例几个消费者
public class DisruptorConfig {

    @Autowired
    private ReactiveRedisTemplate<String, String> template;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CountTool countTool;

    @Autowired
    private TrackTool trackTool;

    @Bean
    public Msghandler msghandler() {
        Msghandler msghandler = new Msghandler(template, mapper, countTool, trackTool);
        return msghandler;
    }

    @Bean
    public SeriesDataEventHandler smsParamEventHandler1() {
        return new SeriesDataEventHandler(msghandler());
    }

}