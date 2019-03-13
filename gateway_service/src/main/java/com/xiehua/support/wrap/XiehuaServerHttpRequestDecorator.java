package com.xiehua.support.wrap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.fun.Try;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;
import java.util.stream.Collectors;

import static com.xiehua.support.wrap.XiehuaServerWebExchangeDecorator.SUPPORT_MEDIA_TYPES;

@Slf4j
public class XiehuaServerHttpRequestDecorator extends ServerHttpRequestDecorator {

    private Flux<DataBuffer> body;


    public XiehuaServerHttpRequestDecorator(ServerHttpRequest delegate) {
        super(delegate);
        final MediaType contentType = delegate.getHeaders().getContentType();
        final String uri = delegate.getURI().toString();
        final String method = Optional.ofNullable(delegate.getMethod()).orElse(HttpMethod.GET).name();
        final String headers = delegate.getHeaders().entrySet().stream().map(s -> s.getKey() + ":[" + String.join(";", s.getValue()) + "]").collect(Collectors.joining("\r\n"));
        StringBuilder builder = new StringBuilder();
        builder.append("请求地址:").append(uri).append("\r\n");
        builder.append("请求方法:").append(method).append("\r\n");
        builder.append("请求头;").append(headers).append("\r\n");
        builder.append("请求体;%s").append("\r\n");
        Flux<DataBuffer> flux = super.getBody();
        if(delegate.getMethod().equals(HttpMethod.GET)) log.info(String.format(builder.toString(), ""));
       if(contentType !=null && SUPPORT_MEDIA_TYPES.stream().anyMatch(s -> s.getType().equalsIgnoreCase(contentType.getType()) && s.getSubtype().equalsIgnoreCase(contentType.getSubtype()))){
            body = flux.map(Try.of(s -> XiehuaServerWebExchangeDecorator.log(builder.toString(),s)));
        }else {
            body = flux;
        }
    }


    @Override
    public Flux<DataBuffer> getBody() {
        return body;
    }


}
