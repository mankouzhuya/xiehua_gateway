package com.xiehua.support.wrap;

import com.xiehua.component.GateWayComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;

import java.io.IOException;
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

    private XiehuaServerHttpRequestDecorator requestDecorator;

    private XiehuaServerHttpResponseDecorator responseDecorator;


    public XiehuaServerWebExchangeDecorator(ServerWebExchange delegate, GateWayComponent gateWayComponent) {
        super(delegate);
        try {
            requestDecorator = new XiehuaServerHttpRequestDecorator(delegate.getRequest(),gateWayComponent);
            responseDecorator = new XiehuaServerHttpResponseDecorator(delegate.getResponse(),gateWayComponent);
        } catch (IOException e) {
            log.error("序列化错误:{}",e);
        }

    }

    @Override
    public ServerHttpRequest getRequest() {
        return requestDecorator;
    }

    @Override
    public ServerHttpResponse getResponse() {
        return responseDecorator;
    }



}
