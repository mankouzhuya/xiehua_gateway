package com.xiehua.config.dto.redis;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class GUser implements Serializable{

    private String gid;//全局id

    private String userIden;//子系统标识

    private LocalDateTime createTime;//创建时间

    private LocalDateTime updateTime;//修改时间

    private List<String> roles;



}
