/**
  * Copyright 2018 bejson.com 
  */
package com.xiehua.config.dto.white_list;
import lombok.ToString;

import java.util.List;

/**
 * Auto-generated: 2018-08-08 13:42:10
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
@ToString
public class WhiteListPermit {

    private String name;
    private String url;
    private List<String> ip;
    public void setName(String name) {
         this.name = name;
     }
     public String getName() {
         return name;
     }

    public void setUrl(String url) {
         this.url = url;
     }
     public String getUrl() {
         return url;
     }

    public void setIp(List<String> ip) {
         this.ip = ip;
     }
     public List<String> getIp() {
         return ip;
     }

}