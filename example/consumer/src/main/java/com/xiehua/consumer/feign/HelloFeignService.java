package com.xiehua.consumer.feign;

import com.xiehua.consumer.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(value = "pay-center",url = "127.0.0.1:5050",path = "pay_center")
public interface HelloFeignService {

    /**
     *
     * @param
     * @return
     */
    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    String hello(@RequestParam(value = "name") String name);


    @RequestMapping(value = "/hello", method = RequestMethod.POST)
    UserDTO hello2(@RequestParam(value = "name") String name, @RequestBody UserDTO userDTO);
}
