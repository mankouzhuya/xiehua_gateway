package com.xiehua.gateway.track;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class Register {

    private Map<String, Enhance> map;

    public Register() {
        log.info("Register初始化了");
        map = new HashMap<>();
    }

    public Register reg(Enhance enhance) {
        log.info("添加:{}",enhance.targetClassName());
        map.put(enhance.targetClassName(), enhance);
        return this;
    }


    public byte[] matchAndProcess(String sourceClassName, ClassPool classPool) throws IOException, CannotCompileException, NotFoundException {
        String temp = sourceClassName.replaceAll("/", ".");
        if (!map.containsKey(temp)) return null;
        return map.get(temp).enhance(temp, classPool);
    }


}
