package com.xiehua.gateway.track.feign;

import com.xiehua.gateway.track.Enhance;
import javassist.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class RequestTemplateEnhance implements Enhance {

    public static final String CLASS_NAME = "feign.RequestTemplate";

    public static final String E_METHOD_REQUEST = "request";

    public static final String HEAD_REQ_ID = "Request-ID";//global request id,write to request head

    public static final String HEAD_ITERM_ID = "Requst-Iterm-ID";//每个单独请求分配一共req id

    public static final String HEAD_FROM_ID = "Requst-From-ID";//每个单独请求分配一共req id

    public static final String HEAD_VERSION = "version";//version

    @Override
    public byte[] enhance(String sourceClassName, ClassPool classPool) throws CannotCompileException, NotFoundException, IOException {
        if (!CLASS_NAME.equals(sourceClassName)) return null;
        CtClass ctClass = classPool.get(CLASS_NAME);
        CtClass.debugDump = "D:/";
        //addTiming(ctClass);
        CtMethod ctMethod = ctClass.getDeclaredMethod(E_METHOD_REQUEST);
         ctMethod.insertBefore(buildMthodSub());
        return ctClass.toBytecode();
    }

    private String buildMthodSub(){
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("org.springframework.web.context.request.RequestAttributes requestAttributes = org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();\r\n");
        stringBuffer.append("javax.servlet.http.HttpServletRequest req = ((org.springframework.web.context.request.ServletRequestAttributes)requestAttributes).getRequest();\r\n");
        //uid
        stringBuffer.append("String reqUid = req.getHeader(\""+HEAD_REQ_ID+"\");\r\n");
        stringBuffer.append("if(reqUid !=null && reqUid !=\"\"){\r\n");
        stringBuffer.append("java.util.List uidList = new java.util.ArrayList();\r\n");
        stringBuffer.append("uidList.add(reqUid);\r\n");
        stringBuffer.append("headers.put(\""+HEAD_REQ_ID+"\",uidList);\r\n");
        stringBuffer.append("}\r\n");

        //itemId
        stringBuffer.append("String itemId = req.getHeader(\""+HEAD_ITERM_ID+"\");\r\n");
        stringBuffer.append("if(itemId !=null && itemId !=\"\"){\r\n");
        stringBuffer.append("java.util.List itemIdList = new java.util.ArrayList();\r\n");
        stringBuffer.append("itemIdList.add(itemId);\r\n");
        stringBuffer.append("headers.put(\""+HEAD_FROM_ID+"\",itemIdList);\r\n");
        stringBuffer.append("}\r\n");

        //version
        stringBuffer.append("String version = req.getHeader(\""+HEAD_VERSION+"\");\r\n");
        stringBuffer.append("if(version !=null && version !=\"\"){\r\n");
        stringBuffer.append("java.util.List versionList = new java.util.ArrayList();\r\n");
        stringBuffer.append("versionList.add(version);\r\n");
        stringBuffer.append("headers.put(\""+HEAD_VERSION+"\",versionList);\r\n");
        stringBuffer.append("}\r\n");

        return stringBuffer.toString();
    }

    @Override
    public String targetClassName() {
        return CLASS_NAME;
    }

    @Override
    public List<String> targetMethodName() {
        return Arrays.asList(E_METHOD_REQUEST);
    }

    private static void addTiming(CtClass cct) throws NotFoundException, CannotCompileException, NotFoundException {
        //获取方法信息,如果方法不存在，则抛出异常
        CtMethod ctMethod = cct.getDeclaredMethod(E_METHOD_REQUEST);
        //将旧的方法名称进行重新命名
        String nname = E_METHOD_REQUEST + "$impl";
        ctMethod.setName(nname);
        //方法的副本，采用过滤器的方式
        CtMethod newCtMethod = CtNewMethod.copy(ctMethod, E_METHOD_REQUEST, cct, null);

        //为该方法添加时间过滤器来计算时间，并判断获取时间的方法是否有返回值
        String type = ctMethod.getReturnType().getName();
        StringBuffer body = new StringBuffer();
        body.append("{\n long start = System.currentTimeMillis();\n");

        body.append("System.out.println(\" "+ nname +"_in_11\");");
        //返回值类型不同
        if(!"void".equals(type)) {
            body.append(type + " result = ");
        }
        //可以通过$$将传递给拦截器的参数，传递给原来的方法
        body.append(nname + "($$);\n");
        //输出方法运行时间差
        body.append("System.out.println(\"Call to method " + nname + " took \" + \n (System.currentTimeMillis()-start) + " +  "\" ms.\");\n");
        body.append("System.out.println(\" "+ nname +"_out_11\");");
        if(!"void".equals(type)) {
            body.append("return result;\n");
        }
        body.append("}");
        //替换拦截器方法的主体内容，并将该方法添加到class之中
        newCtMethod.setBody(body.toString());
        cct.addMethod(newCtMethod);
        System.currentTimeMillis();
    }

}
