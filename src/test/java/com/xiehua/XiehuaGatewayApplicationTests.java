package com.xiehua;

import es.moki.ratelimitj.core.limiter.request.ReactiveRequestRateLimiter;
import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.redis.request.RedisSlidingWindowRequestRateLimiter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

//@RunWith(SpringRunner.class)
//@SpringBootTest
//@DirtiesContext
public class XiehuaGatewayApplicationTests {

    @Test
    public void contextLoads() {


    }

    private static final Logger LOGGER = LoggerFactory.getLogger(XiehuaGatewayApplicationTests.class);


    /**
     * 随便测试下
     */
    @Test
    public void concurrentTest() {

        //这里没有什么用,纯粹是Schedulers.elastic()可以复用这里的线程池,不想写多的代码了
//		Flux.range(1,100).map(a -> a*1).subscribeOn(Schedulers.elastic()).subscribe();

        //开始测试了
        long start = System.currentTimeMillis();


        //第一个参数20 20个并发
        //后面表示N个请求,最长的一个请求可能要2000ms
        list(20, 1000l, 2000l, 100l, 200l, 300l, 400l, 500l, 600l, 700l, 800l, 900l).forEachRemaining(show -> LOGGER.info(show));

        LOGGER.info("总时间 : {} ms", System.currentTimeMillis() - start);

    }

    /**
     * 并行执行
     *
     * @param concurrent 并行数量
     * @param sleeps     模拟停顿时间
     * @return 随便返回了
     */
    public Iterator<String> list(int concurrent, Long... sleeps) {
        return Flux.fromArray(sleeps).log().flatMap(sleep -> Mono.fromCallable(() -> mockHttp(sleep)).subscribeOn(Schedulers.elastic()), concurrent).toIterable().iterator();
    }

    /**
     * 实际上是一个http请求
     *
     * @param sleep 请求耗时
     * @return
     */
    public String mockHttp(long sleep) {
        try {
            Thread.sleep(sleep);
            LOGGER.info("停顿{}ms真的执行了", sleep);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return String.format("停顿了%sms", sleep);
    }

    @Test
    public void testBackpressure() {
        Flux.range(1, 6)    // 1
                .doOnRequest(n -> System.out.println("Request " + n + " values..."))    // 2
                .subscribe(new BaseSubscriber<Integer>() {  // 3
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) { // 4
                        System.out.println("Subscribed and make a request...");
                        request(1); // 5
                    }

                    @Override
                    protected void hookOnNext(Integer value) {  // 6
                        try {
                            TimeUnit.SECONDS.sleep(1);  // 7
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Get value [" + value + "]");    // 8
                        request(1); // 9
                    }
                });
    }

    private static final SecureRandom random = new SecureRandom();

    protected static byte[] randomKey() {
        //create random signing key for testing:
        byte[] key = new byte[64];
        random.nextBytes(key);
        return key;
    }

    @Test
    public void testSetDuplicateSigningKeys() {

//		byte[] keyBytes = randomKey();
//
//		SecretKeySpec key = new SecretKeySpec(keyBytes, "HmacSHA256");
//
//		String compact = Jwts.builder().setPayload("Hello World!").signWith(SignatureAlgorithm.HS256, keyBytes).compact();
//
//		try {
//			Jwts.parser().setSigningKey(keyBytes).setSigningKey(key).parse(compact)
//			fail();
//		} catch (IllegalStateException ise) {
//			ise.printStackTrace();
//		}
    }



    @Test
    public void test_ratelimitj() throws Exception {
//        RedisURI redisURI = RedisURI.Builder.redis("10.200.157.139", 6379).withDatabase(4).withPassword("Zchzredis2017").build();
//
//        RedisClient redisClient = RedisClient.create(redisURI);

//        StatefulRedisConnection<String, String> connection = redisClient.connect();
//
//        Set<RequestLimitRule> rules = new HashSet<>();
//        rules.add(RequestLimitRule.of(1, TimeUnit.SECONDS, 10));
//        rules.add(RequestLimitRule.of(3600, TimeUnit.SECONDS, 240).withPrecision(60));
//
//
//        ReactiveRequestRateLimiter requestRateLimiter = new RedisSlidingWindowRequestRateLimiter(connection, rules);
//
//        Mono<Boolean> observable = requestRateLimiter.overLimitWhenIncrementedReactive("ip:127.0.1.6");
//        observable.subscribe(System.out::println);
//        Thread.sleep(1000);




    }

    @Test
    public void test_2() throws Exception {
        RedisClient client = RedisClient.create("redis://Zchzredis2017@10.200.157.139:6379/4");

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport.createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig());

// executing work
        try (StatefulRedisConnection<String, String> connection = pool.borrowObject()) {

            RedisCommands<String, String> commands = connection.sync();
            commands.multi();
            commands.set("key", "value");
            commands.set("key2", "value2");
            commands.exec();
        }

// terminating
        pool.close();
        client.shutdown();

    }


    private Flux<Integer> generateFluxFrom1To6() {
        return Flux.just(1, 2, 3, 4, 5, 6);
    }
    private Mono<Integer> generateMonoWithError() {
        return Mono.error(new Exception("some error"));
    }
    @Test
    public void testViaStepVerifier() {
        StepVerifier.create(generateFluxFrom1To6())
                .expectNext(1, 2, 3, 4, 5, 6)
                .expectComplete()
                .verify();
        StepVerifier.create(generateMonoWithError())
                .expectErrorMessage("some error")
                .verify();
    }

    @Test
    public void test_3(){
                //JWT
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String key_a = Base64.getEncoder().encodeToString(key.getEncoded());
        System.out.println(key_a);
        //RAS
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS512);
        System.out.println(keyPair.getPublic().getEncoded());

        byte[] keyBytes = Base64.getDecoder().decode("jFCOdJy2Av4PkEPoQj0JGSyBlSbOz7YaGP+NlQVjhlg=");
        SecretKey key2 = Keys.hmacShaKeyFor(keyBytes);

        SecretKeySpec key3 = new SecretKeySpec(keyBytes, "HmacSHA256");

        String jws2 = Jwts.builder()
                .setIssuer("xiehua_gateway")//签发者
                .setSubject("test01")//所面向的用户
                .setAudience("user_name")//接收jwt的一方
                .setExpiration(Date.from(LocalDateTime.now().plusDays(10).atZone(ZoneId.systemDefault()).toInstant())) //jwt的过期时间，这个过期时间必须大于签发时间  Date.from(LocalDateTime.now().plusMinutes(30).atZone(ZoneId.systemDefault()).toInstant())
                .setNotBefore(new Date()) //jwt不可用时间
                .setIssuedAt(new Date()) // 签发时间
                .setId(UUID.randomUUID().toString()).signWith(key2).compact(); //just an example id
        System.out.println(jws2);
       // String jws2 = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJ4aWVodWFfZ2F0ZXdheSIsInN1YiI6ImdpZCIsImF1ZCI6InVzZXJfbmFtZSIsImV4cCI6MTUzMzc4NjI3OCwibmJmIjoxNTMzNzg0NDc4LCJpYXQiOjE1MzM3ODQ0NzgsImp0aSI6IjBmYjk4ZTRhLWQ0MTUtNDY4NS05N2IzLTkwOTQwY2Y5ZDdhZCJ9.cp1ar5kRWraMDWlSSBq6Nb7fzK4O_KeFNCdch_0-si4";
        System.out.println(Jwts.parser().setSigningKey(key2).parseClaimsJws(jws2).getBody().getExpiration());


        System.out.println(LocalDateTime.now().plusDays(1));

    }

}
