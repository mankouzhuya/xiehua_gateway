package com.xiehua.mvc.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CountIndexRespDTO implements Serializable{

    private String userName;

    private List<CountRowDTO> list;
}
