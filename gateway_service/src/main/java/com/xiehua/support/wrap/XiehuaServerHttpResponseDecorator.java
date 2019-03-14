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
import reactor.core.scheduler.Schedulers;

import java.io.IOException;

import static com.xiehua.filter.RouteFilter.HEAD_FROM_ID;
import static com.xiehua.filter.RouteFilter.HEAD_ITERM_ID;
import static com.xiehua.filter.RouteFilter.HEAD_REQ_ID;
import static com.xiehua.support.wrap.XiehuaServerWebExchangeDecorator.SUPPORT_MEDIA_TYPES;
import static com.xiehua.support.wrap.collect.CountTool.ATTR_REQ_ITEM;


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
        if (SUPPORT_MEDIA_TYPES.stream().anyMatch(s -> s.getType().equalsIgnoreCase(contentType.getType()) && s.getSubtype().equalsIgnoreCase(contentType.getSubtype()))) {
            String trackId = getHeaders().getFirst(HEAD_REQ_ID);
            String itemId = getHeaders().getFirst(HEAD_ITERM_ID);
            String fromId = getHeaders().getFirst(HEAD_FROM_ID);

            if (body instanceof Mono) {
                final Mono<DataBuffer> monoBody = (Mono<DataBuffer>) body;
                return super.writeWith(monoBody.publishOn(Schedulers.elastic()).switchIfEmpty(Mono.defer(() -> Mono.empty())).map(Try.of(s -> gateWayComponent.log(s,trackId,itemId,fromId))));
            }
            if (body instanceof Flux) {
                final Flux<DataBuffer> monoBody = (Flux<DataBuffer>) body;
                return super.writeWith(monoBody.publishOn(Schedulers.elastic()).switchIfEmpty(Flux.defer(() -> Flux.empty())).map(Try.of(s -> gateWayComponent.log(s,trackId,itemId,fromId))));
            }
        }

        return super.writeWith(body);
    }

}
