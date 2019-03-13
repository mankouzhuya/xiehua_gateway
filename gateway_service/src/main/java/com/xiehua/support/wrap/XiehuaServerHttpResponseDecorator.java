package com.xiehua.support.wrap;

import com.xiehua.component.GateWayComponent;
import com.xiehua.fun.Try;
import com.xiehua.support.wrap.dto.ReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static com.xiehua.filter.RouteFilter.HEAD_ITERM_ID;
import static com.xiehua.filter.g.CounterFilter.ATTR_REQ_ITEM;
import static com.xiehua.support.wrap.XiehuaServerWebExchangeDecorator.SUPPORT_MEDIA_TYPES;


@Slf4j
public class XiehuaServerHttpResponseDecorator extends ServerHttpResponseDecorator {

    private GateWayComponent gateWayComponent;

    public XiehuaServerHttpResponseDecorator(ServerHttpResponse delegate, GateWayComponent gateWayComponent) {
        super(delegate);
        this.gateWayComponent = gateWayComponent;
    }

    @Override
    public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
        return super.writeAndFlushWith(body);
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        final MediaType contentType = super.getHeaders().getContentType();
        String itemId = getHeaders().getFirst(HEAD_ITERM_ID);
        String key = gateWayComponent.getDefaultCache().genKey(ATTR_REQ_ITEM + itemId);
        String value = gateWayComponent.getDefaultCache().get(key);
        if (!StringUtils.isEmpty(value)){
            try {
                if(SUPPORT_MEDIA_TYPES.stream().anyMatch(s -> s.getType().equalsIgnoreCase(contentType.getType()) && s.getSubtype().equalsIgnoreCase(contentType.getSubtype()))){
                    ReqDTO reqDTO = gateWayComponent.getMapper().readValue(value, ReqDTO.class);
                    if (body instanceof Mono) {
                        final Mono<DataBuffer> monoBody = (Mono<DataBuffer>) body;
                        return super.writeWith(monoBody.switchIfEmpty(Mono.defer(() -> Mono.empty())).map(Try.of(s -> gateWayComponent.log(reqDTO, s, false))));
                    }
                    if (body instanceof Flux) {
                        final Flux<DataBuffer> monoBody = (Flux<DataBuffer>) body;
                        return super.writeWith(monoBody.switchIfEmpty(Flux.defer(() -> Flux.empty())).map(Try.of(s -> gateWayComponent.log(reqDTO, s, false))));
                    }
                }
            } catch (IOException e) {
                log.error("序列化错误:{}", e);
            }finally {
                gateWayComponent.getDefaultCache().remove(key);
            }
        }


        return super.writeWith(body);
    }

}
