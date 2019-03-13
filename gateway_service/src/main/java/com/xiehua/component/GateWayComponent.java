package com.xiehua.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.cache.SimpleCache;
import com.xiehua.support.wrap.dto.ReqDTO;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static com.xiehua.filter.g.CounterFilter.*;

@Slf4j
@Component
public class GateWayComponent {

    @Autowired
    private ReactiveRedisTemplate<String, String> template;

    @Autowired
    private SimpleCache defaultCache;

    @Autowired
    private ObjectMapper mapper;

    public SimpleCache getDefaultCache() {
        return defaultCache;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public <T extends DataBuffer> T log(ReqDTO reqDTO, T buffer, Boolean isReq) throws IOException {
        InputStream dataBuffer = buffer.asInputStream();
        byte[] bytes = IOUtils.toByteArray(dataBuffer);
        // ByteBufAllocator.DEFAULT
        NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(new UnpooledByteBufAllocator(false));
        if(isReq){
            reqDTO.setReqBody(new String(bytes));
            defaultCache.put(reqDTO.getKey(),mapper.writeValueAsString(reqDTO));
        }else {
            LocalDateTime now = LocalDateTime.now();
            reqDTO.setRespBody(new String(bytes));
            reqDTO.setRespTime(now);
            Long executeTime = LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli() - reqDTO.getReqTime().toInstant(ZoneOffset.of("+8")).toEpochMilli();
            reqDTO.setExecuteTime(executeTime);
            log.info("请求响应:{}",mapper.writeValueAsString(reqDTO));
            if(executeTime > DEFAULT_TIMER){//记录耗时较长的接口
                String key = REDIS_GATEWAY_TIMER_REQID_PREFIX + now.toString();
                template.opsForHash()
                        .put(key,reqDTO.getReqId(),mapper.writeValueAsString(reqDTO))
                        .then(template.expire(key, Duration.between(LocalDateTime.now(), LocalDateTime.parse( LocalDate.now().plusDays(7).toString() + " 23:59:59",TIME_FORMATTER))))
                        .subscribe();

            }
        }
        DataBufferUtils.release(buffer);
        return (T) nettyDataBufferFactory.wrap(bytes);
    }
}
