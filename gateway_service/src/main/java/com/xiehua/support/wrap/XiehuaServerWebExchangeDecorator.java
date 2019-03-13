package com.xiehua.support.wrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class XiehuaServerWebExchangeDecorator extends ServerWebExchangeDecorator {

    public static final List<MediaType> SUPPORT_MEDIA_TYPES = Arrays.asList(
            MediaType.APPLICATION_FORM_URLENCODED,
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_JSON_UTF8,
            MediaType.APPLICATION_XHTML_XML,
            MediaType.APPLICATION_XML,
            MediaType.MULTIPART_FORM_DATA,
            MediaType.TEXT_HTML,
            MediaType.TEXT_MARKDOWN,
            MediaType.TEXT_PLAIN,
            MediaType.TEXT_XML);

    public static final String REQ_URL = "req-url";


    private XiehuaServerHttpRequestDecorator requestDecorator;

    private XiehuaServerHttpResponseDecorator responseDecorator;


    public XiehuaServerWebExchangeDecorator(ServerWebExchange delegate) {
        super(delegate);
        requestDecorator = new XiehuaServerHttpRequestDecorator(delegate.getRequest());
        responseDecorator = new XiehuaServerHttpResponseDecorator(delegate.getResponse());
        responseDecorator.getHeaders().put(REQ_URL,Arrays.asList(requestDecorator.getURI().toString()));
    }

    @Override
    public ServerHttpRequest getRequest() {
        return requestDecorator;
    }

    @Override
    public ServerHttpResponse getResponse() {
        return responseDecorator;
    }

    public static <T extends DataBuffer> T log(String content,T buffer) throws IOException {
        InputStream dataBuffer = buffer.asInputStream();
        byte[] bytes = IOUtils.toByteArray(dataBuffer);
        // ByteBufAllocator.DEFAULT
        NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(new UnpooledByteBufAllocator(false));
        String msg = new String(bytes);
        log.info(String.format(content, StringUtils.isEmpty(msg) ? "" : msg));
        DataBufferUtils.release(buffer);
        return (T) nettyDataBufferFactory.wrap(bytes);
    }

}
