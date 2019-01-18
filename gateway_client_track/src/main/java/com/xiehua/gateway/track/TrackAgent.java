package com.xiehua.gateway.track;

import com.xiehua.gateway.track.feign.RequestTemplateEnhance;
import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.NotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

@Slf4j
public class TrackAgent {


    public static void premain(String arg, Instrumentation instrumentation) {
        //if (log.isDebugEnabled()) log.debug("agent 参数:{}", arg);
        Register register = new Register().reg(new RequestTemplateEnhance());
        ClassPool classPool = ClassPool.getDefault();
        instrumentation.addTransformer(new ClassFileTransformer() {
            public byte[] transform(ClassLoader loader, String className, Class classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                //classPool.insertClassPath(new LoaderClassPath(loader));
                classPool.insertClassPath(new ClassClassPath(this.getClass()));
                try {
                    return register.matchAndProcess(className, classPool);
                } catch (CannotCompileException | NotFoundException | IOException e) {
                    log.error("初始化失败:{}", e);
                }
                return null;
            }
        });
        //线程池agent
//        TtlAgent.premain(arg,instrumentation);
    }


}
