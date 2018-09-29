package com.xiehua.config.dto.jwt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Data
public class JwtUser implements UserDetails {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final String gid;//全局id(ssoId)

    private final String account;//账号

    private final String password;

    private final String issue;//签发者

    private final Collection<? extends GrantedAuthority> authorities;

    public JwtUser(
            String gid,
            String account,
            String password,
            String issue,
            Collection<? extends GrantedAuthority> authorities) {
        this.gid = gid;
        this.account = account;
        this.password = password;
        this.issue = issue;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return account;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return true;
    }



	
}
