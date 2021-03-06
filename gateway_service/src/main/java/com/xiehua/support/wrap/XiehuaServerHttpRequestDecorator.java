package com.xiehua.support.wrap;

import com.xiehua.bus.jvm.Bus;
import com.xiehua.component.GateWayComponent;
import com.xiehua.fun.Try;
import com.xiehua.support.wrap.dto.ReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;

import static com.xiehua.support.wrap.XiehuaServerWebExchangeDecorator.SUPPORT_MEDIA_TYPES;

@Slf4j
public class XiehuaServerHttpRequestDecorator extends ServerHttpRequestDecorator {

    private Flux<DataBuffer> body;

    public XiehuaServerHttpRequestDecorator(ServerHttpRequest delegate, ReqDTO reqDTO, GateWayComponent gateWayComponent) throws IOException {
        super(delegate);
        final MediaType contentType = super.getHeaders().getContentType();
        Flux<DataBuffer> flux = super.getBody();
        //先保存一次
        gateWayComponent.saveLocalReqDTO(reqDTO);
        if (contentType != null && SUPPORT_MEDIA_TYPES.stream().anyMatch(s -> s.getType().equalsIgnoreCase(contentType.getType()) && s.getSubtype().equalsIgnoreCase(contentType.getSubtype()))) {
            body = flux.publishOn(Schedulers.elastic()).map(Try.of_f(s -> gateWayComponent.log(s, reqDTO)));
        } else {
            body = flux;
        }
    }


    @Override
    public Flux<DataBuffer> getBody() {
        return body;
    }


}
