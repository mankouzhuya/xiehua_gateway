package com.xiehua.support.wrap.collect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.fun.Try;
import com.xiehua.support.wrap.dto.ReqDTO;
import com.xiehua.support.wrap.dto.track.Span;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.xiehua.support.wrap.dto.ReqDTO.REDIS_GATEWAY_TEMP_PREFIX;

@Slf4j
@Component
public class TrackTool {

    public static final String REDIS_GATEWAY_TRACK = "gateway:track:req_";//redis track

    public static final Long EXP_SECONDS = 60 * 60 * 24 * 1L;//过期时间(1天)

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private StatefulRedisConnection<String, String> connection;

    public void track(ReqDTO reqDTO) throws ExecutionException, InterruptedException, JsonProcessingException {
        RedisAsyncCommands<String, String> commands =  connection.async();
        Map<String,String> map = commands.hgetall(REDIS_GATEWAY_TEMP_PREFIX + reqDTO.getTrackId()).get();
        if(map == null) return ;

        //PriorityQueue<Tuple2<LocalDateTime, LocalDateTime>> queue = newDates.stream().sorted((s, t) -> s._1().compareTo(t._1())).collect(() -> new PriorityQueue(),(s, t) -> s.add(t),(m, n) -> m.addAll(n));
        List<Span> list = map.entrySet()
                .stream()
                .map(Try.of(s ->{
                    ReqDTO t = mapper.readValue(s.getValue(),ReqDTO.class);
                    return new Span(t.getTrackId(), t.getReqId(),t.getFromId(), t.getUrl(), LocalDateTime.now(),t, new ArrayList<>(),t.getExecuteTime());
                })).collect(Collectors.toList());

        Span rootSpan = findRootSpan(list,list.get(0));

        rootSpan = buildTree(list,rootSpan);

        String key = REDIS_GATEWAY_TRACK + reqDTO.getTrackId();
        commands.set(key, mapper.writeValueAsString(rootSpan));
        commands.expire(key,EXP_SECONDS);

    }

    public static void main(String[] args) throws JsonProcessingException {

        Span a = new Span("A","");
        Span b = new Span("B","A");
        Span c = new Span("C","B");
        Span d = new Span("D","B");
        Span e = new Span("E","C");
        List<Span> list = Arrays.asList(a,b,c,d,e);

        TrackTool trackTool = new TrackTool();

        Span rootSpan = trackTool.findRootSpan(list,list.get(2));

         rootSpan = trackTool.buildTree(list,list.get(0));
        System.out.println(new ObjectMapper().writeValueAsString(rootSpan));

    }


    private Span findRootSpan(List<Span> list,Span span){
        List<Span> spans = list.stream().filter(s -> s.getSpanId().equals(span.getFromId())).collect(Collectors.toList());
        if(spans != null && spans.size()>0){
            for(int i =0 ;i< spans.size();i ++){
                return findRootSpan(list,spans.get(i));
            }
        }
        return span;
    }

    private Span buildTree(List<Span> list,Span rootSpan){
        List<Span> spans = list.stream().filter(s -> rootSpan.getSpanId().equals(s.getFromId())).collect(Collectors.toList());
        if(spans != null && spans.size() > 0){
            rootSpan.setChilds(spans);
            spans.forEach(s -> buildTree(list,s));
        }
        return rootSpan;
    }



}
