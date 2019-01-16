package com.xiehua.controller.dto;

import com.xiehua.cache.dto.SimpleKvDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

@Data
public class AddRule2ReqDTO implements Serializable{

    @NotBlank(message = "服务名字不能为空")
    private String service;

    List<SimpleKvDTO> rules;
}
