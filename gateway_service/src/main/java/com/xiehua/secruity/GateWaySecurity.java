package com.xiehua.secruity;

import com.xiehua.authentication.IPFilter;
import com.xiehua.authentication.JwtAuthenticationFilter;
import com.xiehua.component.GateWayComponent;
import com.xiehua.config.dto.CustomConfig;
import com.xiehua.converter.ServerHttpBearerAuthenticationConverter;
import com.xiehua.handle.JWTAuthenticationSuccessHandler;
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

@EnableWebFluxSecurity
public class GateWaySecurity {

    public static List<String> URL_PERMIT_ALL = Arrays.asList("/favicon.ico", "/*.html", "/**/*.html", "/**/*.css", "/**/*.js", "/actuator/**");

    @Autowired
    private CustomConfig customConfig;

    @Autowired
    private GateWayComponent gateWayComponent;

    @Autowired
    private ServerHttpBearerAuthenticationConverter converter;

    @Bean
    public PasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsRepository() {
        UserDetails user = User.builder().username("xiehua_gateway_admin").password("xiehua_gateway_admin").roles("GATEWAY_ADMIN").passwordEncoder(s -> bCryptPasswordEncoder().encode(s)).build();
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
                .pathMatchers(protectUrls.toArray(new String[protectUrls.size()])).hasRole(CustomConfig.SecurityRoleEnum.role_inner_protected.getRole())
                .pathMatchers("/gateway/**").hasRole("GATEWAY_ADMIN")//网关登录配置角色
                .pathMatchers("/order_center/private_sleep/**").hasRole("ADMIN")
                .anyExchange().authenticated()
//                .and()
//                .formLogin()
//                .loginPage("/gateway/login")
                .and()
                .addFilterAt(new IPFilter(customConfig,gateWayComponent), SecurityWebFiltersOrder.FIRST)
                .addFilterAt(new JwtAuthenticationFilter(customConfig, converter), SecurityWebFiltersOrder.HTTP_BASIC)
                .build();
    }


}
