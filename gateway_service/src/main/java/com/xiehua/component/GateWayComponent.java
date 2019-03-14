package com.xiehua.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.cache.SimpleCache;
import com.xiehua.support.wrap.XiehuaServerWebExchangeDecorator;
import com.xiehua.support.wrap.collect.CountTool;
import com.xiehua.support.wrap.collect.TrackTool;
import com.xiehua.support.wrap.dto.ReqDTO;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.xiehua.filter.RouteFilter.HEAD_FROM_ID;
import static com.xiehua.filter.RouteFilter.HEAD_ITERM_ID;
import static com.xiehua.filter.RouteFilter.HEAD_REQ_ID;
import static com.xiehua.support.wrap.collect.CountTool.ATTR_REQ_ITEM;
import static com.xiehua.support.wrap.collect.CountTool.GATEWAY_ATTR_REQ_TIME;
import static com.xiehua.support.wrap.dto.ReqDTO.REDIS_GATEWAY_TEMP_EXP;
import static com.xiehua.support.wrap.dto.ReqDTO.REDIS_GATEWAY_TEMP_PREFIX;

@Slf4j
@Component
public class GateWayComponent {

    @Autowired
    private StatefulRedisConnection<String, String> connection;

    @Autowired
    private SimpleCache defaultCache;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CountTool countTool;

    @Autowired
    private TrackTool trackTool;

    public SimpleCache getDefaultCache() {
        return defaultCache;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    /***
     * 变换请求头
     * */
    public XiehuaServerWebExchangeDecorator mutateWebExchange(ServerWebExchange exchange) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        //spanId
        String spanId = UUID.randomUUID().toString().replace("-", "");
        builder.header(HEAD_ITERM_ID, spanId);
        exchange.getResponse().getHeaders().put(HEAD_ITERM_ID, Arrays.asList(spanId));
        //track id
        String trackId = exchange.getRequest().getHeaders().getFirst(HEAD_REQ_ID);
        if (StringUtils.isEmpty(trackId)) {
            trackId = UUID.randomUUID().toString().replace("-", "");
            builder.header(HEAD_REQ_ID, trackId);
        }
        exchange.getResponse().getHeaders().put(HEAD_REQ_ID, Arrays.asList(trackId));
        //Requst-From-ID
        String reqFromId = exchange.getRequest().getHeaders().getFirst(HEAD_FROM_ID);
        if (!StringUtils.isEmpty(reqFromId)) {
            exchange.getResponse().getHeaders().put(HEAD_FROM_ID, Arrays.asList(reqFromId));
        }
        ServerHttpRequest request = builder.build();
        ServerWebExchange webExchange = exchange.mutate().request(request).response(exchange.getResponse()).build();
        webExchange.getAttributes().put(GATEWAY_ATTR_REQ_TIME, System.currentTimeMillis());

        return new XiehuaServerWebExchangeDecorator(webExchange, buildReqDTO(exchange, spanId), this);

    }

    private ReqDTO buildReqDTO(ServerWebExchange exchange, String itemId) {
        final String uri = exchange.getRequest().getURI().toString();
        final String method = Optional.ofNullable(exchange.getRequest().getMethod()).orElse(HttpMethod.GET).name();
        final String headers = exchange.getRequest().getHeaders().entrySet().stream().map(s -> s.getKey() + ":[" + String.join(";", s.getValue()) + "]").collect(Collectors.joining("\r\n"));
        ReqDTO reqDTO = new ReqDTO();
        reqDTO.setReqId(itemId);
        String key = defaultCache.genKey(ATTR_REQ_ITEM + itemId);
        reqDTO.setKey(key);
        reqDTO.setUrl(uri);
        reqDTO.setMethod(method);
        reqDTO.setHead(headers);
        reqDTO.setReqTime(LocalDateTime.now());
        return reqDTO;
    }

    public <T extends DataBuffer> T log(T buffer, String trackId, String itemId, String fromId) throws IOException, ExecutionException, InterruptedException {
        ReqDTO reqDTO = getReqDTO(itemId);
        if (reqDTO == null) return buffer;

        InputStream dataBuffer = buffer.asInputStream();
        byte[] bytes = IOUtils.toByteArray(dataBuffer);
        // ByteBufAllocator.DEFAULT
        NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(new UnpooledByteBufAllocator(false));
        String content = new String(bytes);
        if (!StringUtils.isEmpty(content)) {
            reqDTO.setRespBody(content);
            reqDTO.setRespTime(LocalDateTime.now());
            Long executeTime = LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli() - reqDTO.getReqTime().toInstant(ZoneOffset.of("+8")).toEpochMilli();
            reqDTO.setExecuteTime(executeTime);
            log.info("请求响应:{}", mapper.writeValueAsString(reqDTO));
            //统计时间
            countTool.countExecuteTime(executeTime, reqDTO);
            //链路信息持久化
            trackTool.track(trackId, fromId, reqDTO, executeTime);
        }
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
            saveReqDTO(reqDTO);
        }
        DataBufferUtils.release(buffer);
        return (T) nettyDataBufferFactory.wrap(bytes);
    }

    public ReqDTO saveReqDTO(ReqDTO reqDTO) {
        try {
            RedisAsyncCommands<String, String> commands = asyncCommands();
            String key = REDIS_GATEWAY_TEMP_PREFIX + reqDTO.getReqId();
            asyncCommands().set(key, mapper.writeValueAsString(reqDTO));
            commands.expire(key, REDIS_GATEWAY_TEMP_EXP);
        } catch (JsonProcessingException e) {
            log.error("持久化失败:{}", e);
        } finally {
            return reqDTO;
        }
    }

    public ReqDTO getReqDTO(String key) throws ExecutionException, InterruptedException, IOException {
        String str = asyncCommands().get(key).get();
        if (StringUtils.isEmpty(str)) return null;
        return mapper.readValue(str, ReqDTO.class);
    }

    private RedisAsyncCommands<String, String> asyncCommands() {
        return connection.async();
    }
}
