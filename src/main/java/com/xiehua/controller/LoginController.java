package com.xiehua.controller;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.xiehua.config.dto.CustomConfig;
import com.xiehua.controller.dto.LoginReqDTO;
import com.xiehua.encrypt.aes.AESUtil;
import com.xiehua.exception.BizException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

import static com.xiehua.converter.ServerHttpBearerAuthenticationConverter.BEARER;

@Controller
@RequestMapping("/gateway/login")
public class LoginController {

    private static final String CODE_SUFFIX = "jpg";

    private static final String COOKIE_CODE_NAME = "gateway_login_img_code";

    @Autowired
    private DefaultKaptcha captchaProducer;

    @Value("${jwt.secret}")
    private String jwtSecreKey;

    @Autowired
    private CustomConfig customConfig;

    @Autowired
    private ReactiveUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @GetMapping("")
    public Mono<String> login(final Model model) {
        return Mono.create(monoSink -> monoSink.success("login"));
    }

    @PostMapping("")
    @ResponseBody
    public Mono<String> authentication(@Validated @RequestBody LoginReqDTO loginReqDTO, @CookieValue(COOKIE_CODE_NAME) String codeS,ServerHttpResponse response) {
        String decryptedPwd = AESUtil.decrypt(Base64.getDecoder().decode(codeS), AESUtil.getRawKey(jwtSecreKey.getBytes()));
        if(!decryptedPwd.equals(loginReqDTO.getCode())) throw new BizException("验证码不正确");
        return userDetailsService.findByUsername(loginReqDTO.getName())
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException("用户不存在"))))
                .filter(s -> passwordEncoder.matches(loginReqDTO.getPwd(),s.getPassword())).switchIfEmpty(Mono.error(new BadCredentialsException("用户名密码不匹配")))
                .map(s ->{
                    String jwt = Jwts.builder()
                            .setIssuer("xiehua_gateway")
                            .setSubject("xiehua_gateway_admin")//gid
                            .setAudience(loginReqDTO.getName())//account
                            .setExpiration(Date.from(LocalDateTime.now().plusDays(10).atZone(ZoneId.systemDefault()).toInstant()))
                            .setNotBefore(new Date())
                            .setIssuedAt(new Date())
                            .setId(loginReqDTO.getPwd()).signWith(customConfig.getJwtSingKey()).compact();//pwd

                    ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(HttpHeaders.AUTHORIZATION,jwt)
                            .httpOnly(true)
                            .domain("localhost")
                            .path("/")
                            .maxAge(60*30);
                    response.addCookie(cookieBuilder.build());
                    response.getHeaders().add(HttpHeaders.AUTHORIZATION, BEARER + jwt);
                    return "登录成功~";
                });
    }


    /**
     * 获取验证码 的 请求路径
     * @param
     * @param
     * @throws Exception
     */
    @GetMapping("/code")
    public ResponseEntity<Resource> defaultKaptcha() throws Exception{
        String code = captchaProducer.createText();
        BufferedImage image =  captchaProducer.createImage(code);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, CODE_SUFFIX, outputStream);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(new ByteArrayResource(outputStream.toByteArray()));
    }

    /**
     * 获取验证码 的 请求路径
     * **/
    @GetMapping("/code2")
    public Mono<Void> defaultKaptcha2(ServerHttpResponse response) throws IOException {
        String code = captchaProducer.createText();
        BufferedImage image =  captchaProducer.createImage(code);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, CODE_SUFFIX, outputStream);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE);
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(COOKIE_CODE_NAME, Base64.getEncoder().encodeToString(AESUtil.encrypt(AESUtil.getRawKey(jwtSecreKey.getBytes()),code)))
                .httpOnly(true)
                .domain("localhost")
                .path("/")
                .maxAge(60*5);
        response.addCookie(cookieBuilder.build());
        byte[] bytes = outputStream.toByteArray();
        return response.writeWith(Flux.create(s ->{
            DataBufferFactory bufferFactory = new DefaultDataBufferFactory();;
            DataBuffer buffer = bufferFactory.allocateBuffer(bytes.length);
            buffer.write(bytes);
            s.next(buffer);
            s.complete();
        }));
    }

}
