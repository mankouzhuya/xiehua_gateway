package com.xiehua.config.secruity.jwt;

import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class XiehuaAuthenticationToken extends UsernamePasswordAuthenticationToken{

    private Claims claims;

    private String serviceName;

    public XiehuaAuthenticationToken(Object principal, Object credentials) {
        super(principal, credentials);
    }

    public XiehuaAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }

    public XiehuaAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities,Claims claims) {
        super(principal, credentials, authorities);
        this.claims = claims;
    }

    public XiehuaAuthenticationToken(Object principal, Object credentials,Claims claims,String serviceName) {
        super(principal, credentials);
        this.claims = claims;
        this.serviceName = serviceName;
    }

    public Claims getClaims() {
        return claims;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setClaims(Claims claims) {
        this.claims = claims;
    }


}
