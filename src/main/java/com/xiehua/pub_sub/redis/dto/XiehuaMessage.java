package com.xiehua.pub_sub.redis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class XiehuaMessage implements Serializable {

    public static final Integer TYPE_CLEAR_ALL = 1;//清空所有缓存

    public static final Integer TYPE_UPDATE_USER_INFO = 10;//更新用户缓存

    public static final Integer TYPE_DELETE_USER_INFO = 11;//删除用户缓存

    public static final Integer TYPE_UPDATE_RULE = 20;//更新路由缓存

    public static final Integer TYPE_DELETE_RULE = 21;//删除路由缓存

    private Integer type;

    private String content;

}
