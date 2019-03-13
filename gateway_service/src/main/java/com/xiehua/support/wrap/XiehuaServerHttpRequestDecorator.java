package com.xiehua.support.wrap;

import com.xiehua.component.GateWayComponent;
import com.xiehua.fun.Try;
import com.xiehua.support.wrap.dto.ReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;

import java.io.IOException;

import static com.xiehua.filter.RouteFilter.HEAD_ITERM_ID;
import static com.xiehua.filter.g.CounterFilter.ATTR_REQ_ITEM;
import static com.xiehua.support.wrap.XiehuaServerWebExchangeDecorator.SUPPORT_MEDIA_TYPES;

@Slf4j
public class XiehuaServerHttpRequestDecorator extends ServerHttpRequestDecorator {

    private Flux<DataBuffer> body;

    public XiehuaServerHttpRequestDecorator(ServerHttpRequest delegate, GateWayComponent gateWayComponent) throws IOException {
        super(delegate);
        final MediaType contentType = super.getHeaders().getContentType();
        Flux<DataBuffer> flux = super.getBody();
        String itemId = getHeaders().getFirst(HEAD_ITERM_ID);
        String key = gateWayComponent.getDefaultCache().genKey(ATTR_REQ_ITEM + itemId);
        String value = gateWayComponent.getDefaultCache().get(key);
        if (!StringUtils.isEmpty(value) && SUPPORT_MEDIA_TYPES.stream().anyMatch(s -> s.getType().equalsIgnoreCase(contentType.getType()) && s.getSubtype().equalsIgnoreCase(contentType.getSubtype()))) {
            ReqDTO reqDTO = gateWayComponent.getMapper().readValue(value, ReqDTO.class);
            body = flux.switchIfEmpty(Flux.defer(() -> Flux.empty())).map(Try.of(s -> gateWayComponent.log(reqDTO, s, true)));
        } else {
            body = flux;
        }
    }


    @Override
    public Flux<DataBuffer> getBody() {
        return body;
    }


}
