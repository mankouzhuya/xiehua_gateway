package com.xiehua.gateway.track;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;

import java.io.IOException;
import java.util.List;

public interface Enhance {

    byte[] enhance(String sourceClassName, ClassPool classPool) throws CannotCompileException, NotFoundException, IOException;

    String targetClassName();

    List<String> targetMethodName();
}
