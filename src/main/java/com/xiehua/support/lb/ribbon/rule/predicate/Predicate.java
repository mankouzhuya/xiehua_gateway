package com.xiehua.support.lb.ribbon.rule.predicate;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class Predicate implements Serializable{



    private String reqId;

    private String serviceName;

    private Map<String,String> visitorMetaData;

    private Map<String,String> serviceMetaData;


}
