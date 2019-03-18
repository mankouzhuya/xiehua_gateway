package com.xiehua.config.secruity;

import com.xiehua.component.GateWayComponent;
import com.xiehua.config.dto.CustomConfig;
import com.xiehua.config.enums.SecurityRoleEnum;
import com.xiehua.config.secruity.jwt.JWTAuthenticationSuccessHandler;
import com.xiehua.config.secruity.jwt.JWTReactiveAuthenticationManager;
import com.xiehua.config.secruity.jwt.JWTSecurityContextRepository;
import com.xiehua.config.secruity.jwt.converter.ServerHttpBearerAuthenticationConverter;
import com.xiehua.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.xiehua.config.dto.white_list.WhiteListPermit.GATEWAY_LOGIN_ACCOUNT;
import static com.xiehua.config.dto.white_list.WhiteListPermit.GATEWAY_LOGIN_PWD;

@EnableWebFluxSecurity
public class GateWaySecurity {

    public static List<String> URL_PERMIT_ALL = Arrays.asList("/favicon.ico", "/*.html", "/**/*.html", "/**/*.css", "/**/*.js", "/actuator/**");

    @Autowired
    private CustomConfig customConfig;

    @Autowired
    private GateWayComponent gateWayComponent;

    @Autowired
    private ServerHttpBearerAuthenticationConverter converter;

    @Autowired
    private JWTReactiveAuthenticationManager authenticationManager;

    @Autowired
    private JWTSecurityContextRepository securityContextRepository;

    @Autowired
    private JWTAuthenticationSuccessHandler authenticationSuccessHandler;

    @Bean
    public PasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsRepository() {
        UserDetails user = User
                .builder()
                .username(GATEWAY_LOGIN_ACCOUNT)
                .password(GATEWAY_LOGIN_PWD)
                .roles(SecurityRoleEnum.role_gateway_admin.getRole())
                .passwordEncoder(s -> bCryptPasswordEncoder().encode(s))
                .build();
        return new MapReactiveUserDetailsService(user);
    }

    @Bean
    public JWTAuthenticationSuccessHandler jwtAuthenticationSuccessHandler() {
        return new JWTAuthenticationSuccessHandler();
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) throws IOException {
        //permit all url
        List<String> permitUrls = Optional.ofNullable(customConfig.getPermitUrls()).orElseThrow(RuntimeException::new).stream().map(s -> s.getUrl()).collect(Collectors.toList());
        permitUrls.addAll(URL_PERMIT_ALL);
        //protect api url
        List<String> protectUrls = Optional.ofNullable(customConfig.getWhiteListPermits()).orElseThrow(RuntimeException::new).stream().map(s -> s.getUrl()).collect(Collectors.toList());

        return http
                .csrf().disable()
                .httpBasic().disable()
                .authorizeExchange()
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .pathMatchers(permitUrls.toArray(new String[permitUrls.size()])).permitAll()
                .pathMatchers(protectUrls.toArray(new String[protectUrls.size()])).hasRole(SecurityRoleEnum.role_inner_protected.getRole())
                .pathMatchers("/xiehua_gateway/**").hasRole(SecurityRoleEnum.role_gateway_admin.getRole())//网关登录配置角色
                .pathMatchers("/order_center/private_sleep/**").hasRole("ADMIN")
                .anyExchange().authenticated()
//                .and()
//                .formLogin()
//                .loginPage("/gateway/login")
                .and()
                .addFilterAt(new JwtAuthenticationFilter(customConfig, gateWayComponent, converter,authenticationManager, securityContextRepository, authenticationSuccessHandler), SecurityWebFiltersOrder.HTTP_BASIC)
                .build();
    }


}
