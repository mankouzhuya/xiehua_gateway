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
package com.xiehua.config.secruity.jwt;

import com.xiehua.cache.SimpleCache;
import com.xiehua.cache.dto.SimpleKvDTO;
import com.xiehua.component.GateWayComponent;
import com.xiehua.config.dto.jwt.JwtUser;
import com.xiehua.config.enums.SecurityRoleEnum;
import com.xiehua.exception.BizException;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.xiehua.config.dto.white_list.WhiteListPermit.*;

/**
 * An authentication manager intended to authenticate a JWT exchange
 * JWT tokens contain all information within the token itself
 * so an authentication manager is not necessary but we provide this
 * implementation to follow a standard.
 * Invalid tokens are filtered one previous step
 */

@Slf4j
@Component
public class JWTReactiveAuthenticationManager implements ReactiveAuthenticationManager {


    public static final String REDIS_GATEWAY_USER_POWER_PREFIX = "gateway:user:power_";//redis user power

    @Value("${spring.application.name:xiehua_gateway}")
    private String applicationName;

    @Autowired
    private GateWayComponent gateWayComponent;

    @Autowired
    private SimpleCache defaultCache;


    /**
     * Successfully authenticate an Authentication object
     *
     * @param authentication A valid authentication object
     * @return authentication A valid authentication object
     */
    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        XiehuaAuthenticationToken xiehuaAuthenticationToken = (XiehuaAuthenticationToken) authentication;
        Object credentials = xiehuaAuthenticationToken.getCredentials();
        if (credentials == null) throw new BizException("票据不能为空");
        if (credentials instanceof String) {
            String credential = (String) credentials;
            if (credential.equals(DEFAULT_WHITE_GID)) {//白名单用户访问
                List<GrantedAuthority> list = Arrays.asList(SecurityRoleEnum.role_inner_protected.getFullRole()).stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                UserDetails userDetails = new JwtUser(DEFAULT_WHITE_GID, DEFAULT_WHITE_ACCOUNT, DEFAULT_WHITE_PWD, applicationName, list);
                return Mono.just(new XiehuaAuthenticationToken(userDetails, DEFAULT_WHITE_GID, userDetails.getAuthorities(), xiehuaAuthenticationToken.getClaims()));
            }
            if (credential.equals(GATEWAY_LOGIN_ACCOUNT)) {//访问web控制台
                List<GrantedAuthority> list = Arrays.asList(SecurityRoleEnum.role_gateway_admin.getFullRole()).stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                UserDetails userDetails = new JwtUser(GATEWAY_LOGIN_GID, GATEWAY_LOGIN_ACCOUNT, GATEWAY_LOGIN_PWD, applicationName, list);
                return Mono.just(new XiehuaAuthenticationToken(userDetails, DEFAULT_WHITE_GID, userDetails.getAuthorities(), xiehuaAuthenticationToken.getClaims()));
            }
        }
        if (credentials instanceof Claims) {
            Claims credential = (Claims) credentials;
            if (StringUtils.isEmpty(credential.getSubject())) throw new BadCredentialsException("票据不合法");
            String grantedAuthoritys = loadLocalCache(credential.getSubject(), xiehuaAuthenticationToken.getServiceName());
            return Mono.just(buildAuthenticationToken(credential, grantedAuthoritys));
        }

        throw new BizException("票据不合法");
    }


    private XiehuaAuthenticationToken buildAuthenticationToken(Claims claims, String grantedAuthoritys) {
        List<GrantedAuthority> list = Arrays.stream(grantedAuthoritys.split(",")).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        UserDetails userDetails = new JwtUser(claims.getSubject(), claims.getAudience(), claims.getId(), claims.getIssuer(), list);
        XiehuaAuthenticationToken authentication = new XiehuaAuthenticationToken(userDetails, claims.getSubject(), userDetails.getAuthorities(), claims);
        return authentication;
    }

    private String genKey(String gid) {
        return REDIS_GATEWAY_USER_POWER_PREFIX + gid;
    }


    //load config form local cache
    private String loadLocalCache(String subject, String host) {
        String redisKey = genKey(subject);
        String localKey = defaultCache.genKey(redisKey);
        Object localValue = defaultCache.get(localKey);
        if (!Objects.isNull(localValue)) {
            List<SimpleKvDTO> userPermissions = (List<SimpleKvDTO>) localValue;
            return getUserPermissions(userPermissions, host);
        }
        //query redis
        List<SimpleKvDTO> userPermissions = gateWayComponent.synGetUserPermissionsByRedis(redisKey);
        if (CollectionUtils.isEmpty(userPermissions)) throw new BadCredentialsException("你没有权限访问该系统");
        //put user permissions to local cache
        defaultCache.put(localKey, userPermissions);
        return getUserPermissions(userPermissions, host);

    }

    private String getUserPermissions(List<SimpleKvDTO> userPermissions, String host) {
        List<String> permissions = userPermissions.stream().filter(s -> s.getKey().equals(host)).map(m -> m.getValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(permissions)) throw new BadCredentialsException("你没有权限访问该系统");
        return permissions.get(0);
    }


}
