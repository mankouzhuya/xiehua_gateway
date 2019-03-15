package com.xiehua.support.wrap;

import com.xiehua.component.GateWayComponent;
import com.xiehua.fun.Try;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
        if (SUPPORT_MEDIA_TYPES.stream().anyMatch(s -> s.getType().equalsIgnoreCase(contentType.getType()) && s.getSubtype().equalsIgnoreCase(contentType.getSubtype()))) {
            final HttpHeaders httpHeaders = getHeaders();
            if (body instanceof Mono) {
                final Mono<DataBuffer> monoBody = (Mono<DataBuffer>) body;
                return super.writeWith(monoBody.publishOn(Schedulers.elastic()).switchIfEmpty(Mono.defer(() -> Mono.empty())).map(Try.of(s -> gateWayComponent.log(s,httpHeaders))));
            }
            if (body instanceof Flux) {
                final Flux<DataBuffer> monoBody = (Flux<DataBuffer>) body;
                return super.writeWith(monoBody.publishOn(Schedulers.elastic()).switchIfEmpty(Flux.defer(() -> Flux.empty())).map(Try.of(s -> gateWayComponent.log(s,httpHeaders))));
            }
        }

        return super.writeWith(body);
    }

}
