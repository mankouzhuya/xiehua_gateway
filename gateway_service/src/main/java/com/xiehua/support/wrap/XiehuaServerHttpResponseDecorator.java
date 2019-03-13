package com.xiehua.support.wrap;

import com.xiehua.fun.Try;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static com.xiehua.support.wrap.XiehuaServerWebExchangeDecorator.REQ_URL;
import static com.xiehua.support.wrap.XiehuaServerWebExchangeDecorator.SUPPORT_MEDIA_TYPES;


@Slf4j
public class XiehuaServerHttpResponseDecorator extends ServerHttpResponseDecorator {

    private Flux<DataBuffer> body;


    public XiehuaServerHttpResponseDecorator(ServerHttpResponse delegate) {
        super(delegate);
    }

    @Override
    public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
        return super.writeAndFlushWith(body);
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        final MediaType contentType = super.getHeaders().getContentType();
        String url = getHeaders().getFirst(REQ_URL);
        if (SUPPORT_MEDIA_TYPES.stream().anyMatch(s -> s.getType().equalsIgnoreCase(contentType.getType()) && s.getSubtype().equalsIgnoreCase(contentType.getSubtype()))){
            if (body instanceof Mono) {
                final Mono<DataBuffer> monoBody = (Mono<DataBuffer>) body;
                this.body = monoBody.flux();
                return super.writeWith(monoBody.map(Try.of(s -> XiehuaServerWebExchangeDecorator.log("地址:" + url + "响应体%s:",s))));
            } else if (body instanceof Flux) {
                final Flux<DataBuffer> monoBody = (Flux<DataBuffer>) body;
                this.body = monoBody;
                return super.writeWith(monoBody.map(Try.of(s -> XiehuaServerWebExchangeDecorator.log("地址:" + url + "响应体%s:",s))));
            }
        }
        return super.writeWith(body);
    }


    public Flux<DataBuffer> getBody() {
        return body;
    }


}
