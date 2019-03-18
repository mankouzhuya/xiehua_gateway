/*
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xiehua.config.secruity.jwt.converter;

import com.xiehua.config.dto.CustomConfig;
import com.xiehua.config.secruity.jwt.XiehuaAuthenticationToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Objects;
import java.util.function.Function;

import static com.xiehua.config.dto.white_list.WhiteListPermit.GATEWAY_LOGIN_ACCOUNT;
import static com.xiehua.filter.Authenticcation.GATEWAY_ATTR_SERVER_NAME;

/**
 * This converter extracts a bearer token from a WebExchange and
 * returns an Authentication object if the JWT token is valid.
 * Validity means is well formed and signature is correct
 */
@Slf4j
@Component
public class ServerHttpBearerAuthenticationConverter implements Function<ServerWebExchange, Mono<XiehuaAuthenticationToken>> {

    public static final String BEARER = "Bearer ";//jwt 头

    public static final String GATEWAY_ATTR_JWT = "gateway_attr_jwt";//geteway attr jwt claims


    @Value("${spring.application.name:xiehua_gateway}")
    private String applicationName;//gateway提供的web管理页面,这是需要特殊处理的服务名字(gateway服务为保留服务名,访问gateway服务是需要特殊处理)

    @Autowired
    private CustomConfig customConfig;


    /**
     * Apply this function to the current WebExchange, an Authentication object
     * is returned when completed.
     *
     * @param serverWebExchange
     * @return
     */
    @Override
    public Mono<XiehuaAuthenticationToken> apply(ServerWebExchange serverWebExchange) {
        return Mono.just(serverWebExchange)
                .map(s -> {//extract token
                    String token = s.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                    if (StringUtils.isEmpty(token)) {
                        HttpCookie cookie = s.getRequest().getCookies().getFirst(HttpHeaders.AUTHORIZATION);
                        if (Objects.isNull(cookie)) throw new BadCredentialsException("没有token或token已失效");
                        token = cookie.getValue();
                    }
                    if (token.contains("%")) token = new String(Base64.getDecoder().decode(token));
                    if (!token.startsWith(BEARER)) token = BEARER + token;
                    return token.substring(BEARER.length(), token.length());
                })
                //validate token
                .filter(n -> !n.isEmpty())
                //parser token
                .map(i -> {
                    String serviceName = (String) serverWebExchange.getAttributes().get(GATEWAY_ATTR_SERVER_NAME);
                    Claims claims = Jwts.parser().setSigningKey(customConfig.getJwtSingKey()).parseClaimsJws(i).getBody();
                    String tempName = applicationName.toUpperCase();
                    if(tempName.contains("_")) tempName = tempName.replace("_","-");
                    if (serviceName.equals(applicationName) || serviceName.equals(tempName)) {//access gateway web console
                        return new XiehuaAuthenticationToken(null, GATEWAY_LOGIN_ACCOUNT,claims,serviceName);
                    } else {
                        return new XiehuaAuthenticationToken(null, claims,claims,serviceName);
                    }
                });

    }


}