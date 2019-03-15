package com.xiehua.support.wrap.collect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.support.wrap.dto.ReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class CountTool {

    public static final String GATEWAY_ATTR_REQ_TIME = "gateway_attr_req_time";//发起请求时间

    public static final String REDIS_GATEWAY_TIMER_REQID_PREFIX = "gateway:timer:count_";

    public static final String ATTR_REQ_ITEM = "attr_req_item";

    public static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final Integer DEFAULT_TIMER = 2000;//接口耗时时长（2秒钟）

    @Autowired
    private ReactiveRedisTemplate<String, String> template;

    @Autowired
    private ObjectMapper mapper;


    /**
     * 计算执行时间
     **/
    public void countExecuteTime(ReqDTO reqDTO) throws JsonProcessingException {
        if (reqDTO.getExecuteTime() < DEFAULT_TIMER) return;
        //记录耗时较长的接口
        String key = REDIS_GATEWAY_TIMER_REQID_PREFIX + LocalDate.now().toString();
        template.opsForHash()
                .put(key, reqDTO.getReqId(), mapper.writeValueAsString(reqDTO))
                .then(template.expire(key, Duration.between(LocalDateTime.now(), LocalDateTime.parse(LocalDate.now().plusDays(7).toString() + " 23:59:59", TIME_FORMATTER))))
                .subscribe();
    }

}
