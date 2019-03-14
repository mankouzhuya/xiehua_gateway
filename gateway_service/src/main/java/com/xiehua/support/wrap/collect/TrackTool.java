package com.xiehua.support.wrap.collect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.fun.Try;
import com.xiehua.support.wrap.dto.ReqDTO;
import com.xiehua.track.Span;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TrackTool {

    public static final String REDIS_GATEWAY_TRACK = "gateway:track:req_";//redis track

    public static final Long EXP_SECONDS = 60 * 5L;//过期时间(5分钟)

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ReactiveRedisTemplate<String, String> template;

    public void track(String traceId,String fromId,ReqDTO reqDTO,Long executeTime){
        if(StringUtils.isEmpty(traceId)) return ;
        if(StringUtils.isEmpty(fromId)) fromId = reqDTO.getReqId();
        String key = REDIS_GATEWAY_TRACK + traceId;
        Span currentSpan = new Span(traceId, fromId, reqDTO.getUrl(), LocalDateTime.now(),reqDTO, new ArrayList<>(),executeTime);
        asyncRW(key,currentSpan);
    }

    private void asyncRW(String key, Span currentSpan) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusSeconds(EXP_SECONDS);
        Duration exp = Duration.between(now, end);

        template.opsForValue().get(key).switchIfEmpty(Mono.defer(() ->{
            try {
                return template.opsForValue().set(key,mapper.writeValueAsString(currentSpan),exp).then(Mono.empty());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return Mono.empty();
        })).flatMap(Try.of(s ->{
            Span rootSpan = mapper.readValue(s, new TypeReference<Span>() { });
            Span parentSpan = findParentSpan(rootSpan,currentSpan.getSpanId());
            if(parentSpan == null) return Mono.just(Boolean.FALSE);
            parentSpan.addChild(currentSpan);
            return template.opsForValue().set(key,mapper.writeValueAsString(rootSpan),exp);
        })).subscribe();

    }

    private Span findParentSpan(Span span,String spanId){
        if(spanId.equals(span.getSpanId())) return span;
        List<Span> childs = span.getChilds();
        if(childs != null && childs.size() > 1){
            for(int i = 0;i< childs.size(); i++){
                Span temp = childs.get(i);
                if(spanId.equals(temp.getSpanId())) return temp;
                findParentSpan(temp,spanId);
            }
        }
        return null;
    }
}
