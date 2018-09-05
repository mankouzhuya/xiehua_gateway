package com.xiehua.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class IndexRespDTO implements Serializable{

    private String userName;

    private List<Service> serviceList;


    public IndexRespDTO(String userName){
        this.userName = userName;
    }

    @Data
    public static class Service implements Serializable{

        private String serviceName;

        private List<Rule> roles;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rule implements Serializable{

        private String clientKey;

        private String clientValue;

        private String serverKey;

        private String serverValue;
    }

}
