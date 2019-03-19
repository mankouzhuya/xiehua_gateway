package com.xiehua.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.bus.jvm.Bus;
import com.xiehua.cache.SimpleCache;
import com.xiehua.cache.dto.SimpleKvDTO;
import com.xiehua.config.dto.CustomConfig;
import com.xiehua.support.wrap.XiehuaServerWebExchangeDecorator;
import com.xiehua.support.wrap.dto.ReqDTO;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.vavr.Tuple2;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.xiehua.support.wrap.dto.ReqDTO.*;

@Slf4j
@Component
public class GateWayComponent {

    public static final String REQ_ORDER = "Req_Order";//请求顺序

    public static final String HEAD_REQ_ID = "Request-ID";//global request id,write to request head

    public static final String HEAD_ITERM_ID = "Requst-Iterm-ID";//每个单独请求分配一共req id

    public static final String HEAD_FROM_ID = "Requst-From-ID";//每个单独请求分配一共req id

    @Autowired
    private StatefulRedisConnection<String, String> connection;

    @Autowired
    private ReactiveRedisTemplate<String, String> template;

    @Autowired
    private SimpleCache defaultCache;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CustomConfig config;

    public SimpleCache getDefaultCache() {
        return defaultCache;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    /***
     * 变换请求头
     * */
    public ServerWebExchange mutateWebExchange(ServerWebExchange exchange) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        //spanId
        String spanId = UUID.randomUUID().toString().replace("-", "");
        builder.header(HEAD_ITERM_ID, spanId);
        exchange.getResponse().getHeaders().put(HEAD_ITERM_ID, Arrays.asList(spanId));
        //Requst-From-ID
        String reqFromId = exchange.getRequest().getHeaders().getFirst(HEAD_FROM_ID);
        if (!StringUtils.isEmpty(reqFromId)) {
            exchange.getResponse().getHeaders().put(HEAD_FROM_ID, Arrays.asList(reqFromId));
            exchange.getResponse().getHeaders().put(HEAD_ITERM_ID, Arrays.asList(reqFromId));
        }
        //是否采样
        //track id
        String  trackId= exchange.getRequest().getHeaders().getFirst(HEAD_REQ_ID);
        //trackId为空
        if(StringUtils.isEmpty(trackId)){
            trackId = UUID.randomUUID().toString().replace("-", "");
            exchange.getResponse().getHeaders().put(REQ_ORDER, Arrays.asList("1"));
            BigDecimal seed = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(1));
            if(seed.compareTo(config.getCustomerSamplingRate()) < 0){//需要采样
                trackId+="@";
                return buildExchange(builder,exchange,trackId,spanId,reqFromId,true);
            }
            //不需要采样
            return buildExchange(builder,exchange,trackId,spanId,reqFromId,false);
        }
        //trackId不为空
        if(trackId.endsWith("@")){//需要采样
            return buildExchange(builder,exchange,trackId,spanId,reqFromId,true);
        }
        return buildExchange(builder,exchange,trackId,spanId,reqFromId,false);
    }

    private ServerWebExchange buildExchange(ServerHttpRequest.Builder builder,ServerWebExchange exchange,String trackId,String spanId,String reqFromId,Boolean isSample){
        builder.header(HEAD_REQ_ID, trackId);
        exchange.getResponse().getHeaders().put(HEAD_REQ_ID, Arrays.asList(trackId));

        ServerHttpRequest request = builder.build();
        exchange = exchange.mutate().request(request).response(exchange.getResponse()).build();
        if(isSample){
            return new XiehuaServerWebExchangeDecorator(exchange, buildReqDTO(exchange, spanId, trackId,reqFromId), this);
        }
        return exchange;
    }

    private ReqDTO buildReqDTO(ServerWebExchange exchange, String itemId, String trackId,String fromId) {
        final String uri = exchange.getRequest().getURI().toString();
        final String method = Optional.ofNullable(exchange.getRequest().getMethod()).orElse(HttpMethod.GET).name();
        //final String headers = exchange.getRequest().getHeaders().entrySet().stream().map(s -> s.getKey() + ":[" + String.join(";", s.getValue()) + "]").collect(Collectors.joining("\r\n"));
        ReqDTO reqDTO = new ReqDTO();
        reqDTO.setReqId(itemId);
        reqDTO.setTrackId(trackId);
        reqDTO.setFromId(fromId);
        reqDTO.setUrl(uri);
        reqDTO.setMethod(method);
        reqDTO.setReqhead(readReq2Map(exchange));
        reqDTO.setReqTime(LocalDateTime.now());
        reqDTO.setType(TYPE_SAVE_TEMP);
        return reqDTO;
    }

    public <T extends DataBuffer> T log(T buffer, HttpHeaders respHeaders) throws IOException, ExecutionException, InterruptedException {
        String trackId = respHeaders.getFirst(HEAD_REQ_ID);
        String itemId = respHeaders.getFirst(HEAD_ITERM_ID);
        String fromId = respHeaders.getFirst(HEAD_FROM_ID);
        ReqDTO reqDTO = getReqDTO(REDIS_GATEWAY_TEMP_PREFIX + trackId, itemId);
        String reqOrder = respHeaders.getFirst(REQ_ORDER);
        if (reqDTO == null) return buffer;

        InputStream dataBuffer = buffer.asInputStream();
        byte[] bytes = IOUtils.toByteArray(dataBuffer);
        // ByteBufAllocator.DEFAULT
        NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(new UnpooledByteBufAllocator(false));
        String content = new String(bytes);
        reqDTO.setRespBody(content);
        reqDTO.setRespTime(LocalDateTime.now());
        reqDTO.setResphead(readReq2Map(respHeaders));
        Long executeTime = LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli() - reqDTO.getReqTime().toInstant(ZoneOffset.of("+8")).toEpochMilli();
        reqDTO.setExecuteTime(executeTime);
        reqDTO.setFromId(fromId);
        log.info("请求响应:{}", mapper.writeValueAsString(reqDTO));
        reqDTO.setType(TYPE_SAVE_TEMP);
        if (!StringUtils.isEmpty(reqOrder)) {//第一个请求响应结束时在去持久化链路信息
            HashMap<String, Object> map = new HashMap();
            map.put("fromId", fromId);
            map.put("reqOrder", reqOrder);
            reqDTO.setBizMap(map);
        }
        //更新响应体
        Bus.post(reqDTO);
        //统计时间
        ReqDTO reqDTO2 = new ReqDTO();
        BeanUtils.copyProperties(reqDTO, reqDTO2);
        reqDTO2.setType(TYPE_COUNT_EXEC_TIME);
        Bus.post(reqDTO2);
        DataBufferUtils.release(buffer);
        return (T) nettyDataBufferFactory.wrap(bytes);
    }

    /**
     * 打log
     **/
    public <T extends DataBuffer> T log(T buffer, ReqDTO reqDTO) throws IOException {
        InputStream dataBuffer = buffer.asInputStream();
        byte[] bytes = IOUtils.toByteArray(dataBuffer);
        // ByteBufAllocator.DEFAULT
        NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(new UnpooledByteBufAllocator(false));
        String content = new String(bytes);
        if (!StringUtils.isEmpty(content)) {
            reqDTO.setReqBody(content);
            Bus.post(reqDTO);
        }
        DataBufferUtils.release(buffer);
        return (T) nettyDataBufferFactory.wrap(bytes);
    }

    public ReqDTO getReqDTO(String key, String field) throws ExecutionException, InterruptedException, IOException {
        String str = asyncCommands().hget(key, field).get();
        if (StringUtils.isEmpty(str)) return null;
        return mapper.readValue(str, ReqDTO.class);
    }

    /**
     * read web exchange to map
     **/
    public static Map<String, String> readReq2Map(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().entrySet().stream().map(s -> {
            Tuple2<String, String> t = new Tuple2(s.getKey(), s.getValue().get(0));
            return t;
        }).collect(Collectors.toMap(s -> s._1, t -> t._2, (x, y) -> y));
    }

    /**
     * read web exchange to map
     **/
    public static Map<String, String> readReq2Map(HttpHeaders respHeaders) {
        return respHeaders.entrySet().stream().map(s -> {
            Tuple2<String, String> t = new Tuple2(s.getKey(), s.getValue().get(0));
            return t;
        }).collect(Collectors.toMap(s -> s._1, t -> t._2, (x, y) -> y));
    }

    private RedisAsyncCommands<String, String> asyncCommands() {
        return connection.async();
    }

    public List<SimpleKvDTO> synGetUserPermissionsByRedis(String redisKey){
        ReactiveHashOperations<String, String, String> hashOperations = template.opsForHash();
        return hashOperations.entries(redisKey).map(s -> {
            SimpleKvDTO dto = new SimpleKvDTO();
            dto.setKey(s.getKey());
            dto.setValue(s.getValue());
            return dto;
        }).collectList().block();
    }
}
