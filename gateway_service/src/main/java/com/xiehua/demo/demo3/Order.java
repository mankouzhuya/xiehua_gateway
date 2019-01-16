package com.xiehua.demo.demo3;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class Order implements Serializable{

    private String num = UUID.randomUUID().toString();

    private Integer price;


    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }
}
