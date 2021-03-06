package com.xiehua.config.dto;

import com.xiehua.config.dto.security.SecurityPermitUrl;
import com.xiehua.config.dto.white_list.WhiteListPermit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.util.List;

public class CustomConfig {

    private List<SecurityPermitUrl> permitUrls;

    private List<WhiteListPermit> whiteListPermits;

    private SecretKey jwtSingKey;

    private Integer jwtExpiration;

    private BigDecimal customerSamplingRate;//采样率

    private String applicationName;

    public CustomConfig(List<SecurityPermitUrl> permitUrls, List<WhiteListPermit> whiteListPermits) {
        this.permitUrls = permitUrls;
        this.whiteListPermits = whiteListPermits;
    }

    public List<SecurityPermitUrl> getPermitUrls() {
        return permitUrls;
    }

    public void setPermitUrls(List<SecurityPermitUrl> permitUrls) {
        this.permitUrls = permitUrls;
    }

    public List<WhiteListPermit> getWhiteListPermits() {
        return whiteListPermits;
    }

    public void setWhiteListPermits(List<WhiteListPermit> whiteListPermits) {
        this.whiteListPermits = whiteListPermits;
    }

    public BigDecimal getCustomerSamplingRate() {
        return customerSamplingRate;
    }

    public void setCustomerSamplingRate(BigDecimal customerSamplingRate) {
        this.customerSamplingRate = customerSamplingRate;
    }

    public SecretKey getJwtSingKey() {
        return jwtSingKey;
    }

    public void setJwtSingKey(SecretKey jwtSingKey) {
        this.jwtSingKey = jwtSingKey;
    }

    public Integer getJwtExpiration() {
        return jwtExpiration;
    }

    public void setJwtExpiration(Integer jwtExpiration) {
        this.jwtExpiration = jwtExpiration;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
}
