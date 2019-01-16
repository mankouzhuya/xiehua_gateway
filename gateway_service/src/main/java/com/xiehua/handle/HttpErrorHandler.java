package com.xiehua.handle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.xiehua.exception.BizException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class HttpErrorHandler extends DefaultResponseErrorHandler {

    private final Logger logger = LoggerFactory.getLogger(HttpErrorHandler.class);

    @Autowired
    @Qualifier("objectMapper")
    private ObjectMapper mapper;

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        String body = IOUtils.toString(response.getBody(),StandardCharsets.UTF_8.name());
        logger.error("远程服务调用出错httpCode:{},httpBody:{}",response.getStatusCode(),body);
        if(response.getStatusCode().equals(HttpStatus.BAD_REQUEST)){
            throw mapper.readValue(body,BizException.class);
        }
        if(response.getStatusCode().equals(HttpStatus.UNAUTHORIZED)){
            throw new UsernameNotFoundException("请求凭证不存在");
        }
        if(response.getStatusCode().equals(HttpStatus.FORBIDDEN)){
            throw new AccessDeniedException("权限不够");
        }
        throw new RuntimeException("服务器开小差");
    }

}