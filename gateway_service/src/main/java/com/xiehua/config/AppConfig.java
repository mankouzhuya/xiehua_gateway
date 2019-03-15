package com.xiehua.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.xiehua.config.dto.CustomConfig;
import com.xiehua.config.dto.security.SecurityPermitUrl;
import com.xiehua.config.dto.white_list.WhiteListPermit;
import io.jsonwebtoken.security.Keys;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class AppConfig {

    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    @Value("${http.maxTotal:30}")
    private int maxTotal;

    @Value("${http.maxPerRoute:30}")
    private int maxPerRoute;

    @Value("${http.connectTimeout:30000}")
    private int connectTimeout;

    @Value("${http.readTimeout:30000}")
    private int readTimeout;

    @Value("${http.requestTimeout:15000}")
    private int requestTimeout;

    @Value("${jwt.secret}")
    private String jwtSecreKey;

    @Value("${jwt.expiration}")
    private Integer jwtExpiration;

    @Value("${security.permitUrl}")
    private String securityPermitUrl;

    @Value("${whiteList.permit}")
    private String whiteListPermit;

    @Value("${customer.sampling.rate:1%}")
    private String customerSamplingRate;


    @Bean
    @ConditionalOnMissingBean
    public HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory() {
        // 长连接保持30秒
        PoolingHttpClientConnectionManager pollingConnectionManager = new PoolingHttpClientConnectionManager(30, TimeUnit.SECONDS);
        pollingConnectionManager.setMaxTotal(maxTotal);
        pollingConnectionManager.setDefaultMaxPerRoute(maxPerRoute);

        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        httpClientBuilder.setConnectionManager(pollingConnectionManager);
        httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(2, true));
        // 保持长连接配置，需要在头添加Keep-Alive
        httpClientBuilder.setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE);
        // RequestConfig.Builder builder = RequestConfig.custom();
        // builder.setConnectionRequestTimeout(200);
        // builder.setConnectTimeout(5000);
        // builder.setSocketTimeout(5000);
        // RequestConfig requestConfig = builder.build();
        // httpClientBuilder.setDefaultRequestConfig(requestConfig);

        HttpClient httpClient = httpClientBuilder.build();

        // httpClient连接配置，底层是配置RequestConfig
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        clientHttpRequestFactory.setConnectTimeout(connectTimeout);
        clientHttpRequestFactory.setReadTimeout(readTimeout);
        clientHttpRequestFactory.setConnectionRequestTimeout(requestTimeout);
        // clientHttpRequestFactory.setBufferRequestBody(false);
        return clientHttpRequestFactory;
    }

    @Bean("objectMapper")
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 忽略json字符串中不识别的属性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 忽略无法转换的对象 “No serializer found for class com.xxx.xxx”
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        //取消timestamps形式
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.findAndRegisterModules();
        return objectMapper;
    }

    @Bean
    @ConditionalOnMissingBean
    public PathMatchingResourcePatternResolver getPathMatchingResourcePatternResolver() {
        return new PathMatchingResourcePatternResolver();
    }

//    @Bean
//    @ConditionalOnBean(DiscoveryClient.class)
//    @ConditionalOnProperty(name = "spring.cloud.gateway.discovery.locator.enabled")
//    public DiscoveryClientRouteDefinitionLocator discoveryClientRouteDefinitionLocator(DiscoveryClient discoveryClient, DiscoveryLocatorProperties properties) {
//        return new DiscoveryClientRouteDefinitionLocator(discoveryClient, properties);
//    }


    @Bean
    public CustomConfig customConfig(ObjectMapper mapper) {
        CustomConfig config = new CustomConfig(geUrltPermit(mapper), getIpPermit(mapper));
        config.setJwtExpiration(jwtExpiration);
        config.setJwtSingKey(Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecreKey)));
        //采样率
        config.setCustomerSamplingRate(BigDecimal.valueOf(Double.parseDouble(customerSamplingRate.replace("%", ""))*0.01));
        return config;
        // return new CustomConfig(null,null);
    }

    private List<SecurityPermitUrl> geUrltPermit(ObjectMapper mapper) {
        try {
            return mapper.readValue(securityPermitUrl, new TypeReference<List<SecurityPermitUrl>>() {
            });
        } catch (IOException e) {
            logger.error("解析sso config 配置项permit url错误", e);
            return null;
        }
    }

    private List<WhiteListPermit> getIpPermit(ObjectMapper mapper) {
        try {
            return mapper.readValue(whiteListPermit, new TypeReference<List<WhiteListPermit>>() {
            });
        } catch (IOException e) {
            logger.error("解析sso config 配置项ip permit错误", e);
            return null;
        }
    }


}
