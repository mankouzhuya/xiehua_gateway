package com.xiehua.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.config.dto.redis.GUser;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfiguration {

    @Autowired
    private ObjectMapper mapper;

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private Integer port;

    @Value("${spring.redis.password}")
    private String password;

    @Value("${spring.redis.database}")
    private Integer database;


    private static final String REDIS_PROTOCOL = "redis://";

//    @Bean
//    public LettuceConnectionFactory redisConnectionFactory() {
//        return new LettuceConnectionFactory();
//    }
//
//    @Bean(name = "redisPool")
//    public GenericObjectPool<StatefulRedisConnection<String, String>> getRedisConnectionPool(){
//        String uri = new StringBuffer().append(REDIS_PROTOCOL).append(password).append("@").append(host).append(":").append(port).append("/").append(database).toString();
//        RedisClient client = RedisClient.create(uri);
//        //ConnectionPool pool = new ConnectionPool(new ConnectionPoolSupport.RedisPooledObjectFactory(connectionSupplier), new GenericObjectPoolConfig());
//
//        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport.createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig());
//        return pool;
//    }
//
    @Bean
    public StatefulRedisConnection<String,String> getStatefulRedisConnection(){
        String uri = new StringBuffer().append(REDIS_PROTOCOL).append(password).append("@").append(host).append(":").append(port).append("/").append(database).toString();
        RedisClient client = RedisClient.create(uri);
        return client.connect();
    }


    /**
     * Configures a {@link ReactiveRedisTemplate} with {@link String} keys and a typed
     * {@link Jackson2JsonRedisSerializer}.
     */
    @Bean(name = "redisTemplateGUser")
    public ReactiveRedisTemplate<String, GUser> reactiveJsonPersonRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        Jackson2JsonRedisSerializer<GUser> serializer = new Jackson2JsonRedisSerializer<>(GUser.class);
        serializer.setObjectMapper(mapper);
        RedisSerializationContext.RedisSerializationContextBuilder<String, GUser> builder = RedisSerializationContext .newSerializationContext(new StringRedisSerializer());
        RedisSerializationContext<String, GUser> serializationContext = builder.value(serializer).build();
        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }

    /**
     * Configures a {@link ReactiveRedisTemplate} with {@link String} keys and {@link GenericJackson2JsonRedisSerializer}.
     */
    @Bean(name = "redisTemplateObject")
    public ReactiveRedisTemplate<String, Object> reactiveJsonObjectRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        RedisSerializationContext.RedisSerializationContextBuilder<String, Object> builder = RedisSerializationContext.newSerializationContext(new StringRedisSerializer());
        RedisSerializationContext<String, Object> serializationContext = builder.value(new GenericJackson2JsonRedisSerializer("_type")).build();
        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }


}
