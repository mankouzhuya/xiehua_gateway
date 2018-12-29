package com.xiehua.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.pub_sub.redis.XiehuaRedisListener;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
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
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Map;

import static com.xiehua.cache.DefaultCache.REDIS_GATEWAY_UPDATE_LOCALCACHE_TOPIC;

@Configuration
public class RedisConfiguration {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private LettuceConnectionFactory lettuceConnectionFactory;

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private Integer port;

    @Value("${spring.redis.password}")
    private String password;

    @Value("${spring.redis.database}")
    private Integer database;


    public static final String REDIS_PROTOCOL = "redis://";

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
        ClientResources res = DefaultClientResources.builder()
                .ioThreadPoolSize(4)
                .computationThreadPoolSize(4)
                .build();
        String uri = new StringBuffer().append(REDIS_PROTOCOL).append(password).append("@").append(host).append(":").append(port).append("/").append(database).toString();
        RedisClient client = RedisClient.create(res,uri);
        return client.connect();
    }


    //刚才的监听器
    @Bean
    public XiehuaRedisListener getConsumerRedis() {
        return new XiehuaRedisListener();
    }

    @Bean
    public ChannelTopic appTopic() {
        return new ChannelTopic(REDIS_GATEWAY_UPDATE_LOCALCACHE_TOPIC);
    }

    //让监听器监听关心的话题
    @Bean
    public RedisMessageListenerContainer setRedisMessageListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(lettuceConnectionFactory);
        //话题1
        container.addMessageListener(getConsumerRedis()	, appTopic());
        return container;
    }

    /**
     * Configures a {@link ReactiveRedisTemplate} with {@link String} keys and a typed
     * {@link Jackson2JsonRedisSerializer}.
     */
//    @Bean(name = "redisTemplateGUser")
//    public ReactiveRedisTemplate<String, GUser> reactiveJsonPersonRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
//        Jackson2JsonRedisSerializer<GUser> serializer = new Jackson2JsonRedisSerializer<>(GUser.class);
//        serializer.setObjectMapper(mapper);
//        RedisSerializationContext.RedisSerializationContextBuilder<String, GUser> builder = RedisSerializationContext .newSerializationContext(new StringRedisSerializer());
//        RedisSerializationContext<String, GUser> serializationContext = builder.value(serializer).build();
//        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
//    }

//    @Bean
//    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory){
//        return new ReactiveRedisTemplate<String, String>(connectionFactory, RedisSerializationContext.string());
//    }




}
