package com.xiehua.utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiehua.encrypt.aes.AESUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebUtil {

    private static final Logger logger = LoggerFactory.getLogger(WebUtil.class);

    // kisso默认密码
    public static final String DEFAULT_PASSWORD = "eyJhdXRoX3Rva2VuIjogIk4xK044SG9QTHZMQW";
    // 正则表达式 手机号码
    public static final String EXP_MOBILE_PHONE = "^1\\d{10}$";
    // 正则表达式 中文姓名
    public static final String EXP_CHINESE_NAME = "^[\u4E00-\u9FA5]{2,32}(?:·[\u4E00-\u9FA5]{2,32})?$";
    // 密码规则:6-12位数字和字母的组合
    public static final String EXP_PWD_RULE = "^(?![A-Z]+$)(?![a-z]+$)(?!\\d+$)(?![\\W_]+$)\\S{6,20}$";

    public static final DateTimeFormatter DATE_YYYY_MM_DD_HH_MM_SS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String API_KEY_SIGN = "sign";

    public static final String API_KEY_SYS = "sys";

    public static final String API_MAP_SIZE = "gSize";

    public static final String API_MAP_TYPE = "gType";

    public static final String API_MAP_TIME = "gTime";

    public static final String API_REQ_TYPE_VOID = "Void";

    public static final String API_REQ_TYPE_COLLECTION = "Collection";

    public static final String API_REQ_TYPE_MAP = "Map";

    public static final String API_REQ_TYPE_NUMBER = "Number";

    public static final String API_REQ_TYPE_STRING = "String";

    /**
     * 校验中文姓名
     **/
    public static boolean isChinaseName(String name) {
        if (name == null) return false;
        return name.matches(EXP_CHINESE_NAME);
    }

    /**
     * 校验手机号码
     **/
    public static boolean isMoiblePhone(String mobile) {
        if (mobile == null) return false;
        return mobile.matches(EXP_MOBILE_PHONE);
    }

    /**
     * 去除str 中的空格
     **/
    public static String replaceBlank(String str) {
        String dest = "";
        if (str != null) {
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(str);
            dest = m.replaceAll("");
        }
        return dest;
    }


    public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException, IllegalAccessException {


    }

    /**
     * api调用时，生成QueryStr
     **/
    public static String genQueryStr(Object obj, String key, String sysName) throws IllegalAccessException, UnsupportedEncodingException {
        String sign = genSign(obj, key);
        StringBuffer buffer = new StringBuffer();
        buffer.append(API_KEY_SIGN);
        buffer.append("=");
        buffer.append(sign);
        buffer.append("&");
        buffer.append(API_KEY_SYS);
        buffer.append("=");
        buffer.append(sysName);
        return buffer.toString();
    }

    /**
     * api调用时，生成sign参数
     **/
    public static String genSign(Object obj, String key) throws IllegalAccessException, UnsupportedEncodingException {
        if (key == null || key == "") throw new IllegalAccessException("参数key不能为空");
        if (obj == null) {
            Map<String, String> map = new HashMap<>();
            map.put(API_MAP_SIZE, "0");
            map.put(API_MAP_TYPE, API_REQ_TYPE_VOID);
            map.put(API_MAP_TIME, LocalDateTime.now().format(DATE_YYYY_MM_DD_HH_MM_SS));
            return sign(map, key);
        }
        Map<String, String> map = beanToMapWithString(obj, false);
        map.put(API_MAP_TIME, LocalDateTime.now().format(DATE_YYYY_MM_DD_HH_MM_SS));
        return sign(map, key);
    }

    /**
     * api调用时，生成sign参数
     **/
    public static String sign(Map<String, String> map, String key) throws UnsupportedEncodingException {
        String source = toQueryString(map, true);
        if (logger.isDebugEnabled()) logger.debug("待签名数据为:{}", source);
        if (logger.isDebugEnabled()) logger.debug("key数据为:{}", key);
        // 密钥数据
        byte[] rawKey = AESUtil.getRawKey(key.getBytes());
        // 密码加密后的密文
        byte[] encryptedByteArr = AESUtil.encrypt(rawKey, source);
        String hashed = Base64.getEncoder().encodeToString(encryptedByteArr);
        if (logger.isDebugEnabled()) logger.debug("签名后数据为:{}", hashed);
        return URLEncoder.encode(hashed, StandardCharsets.UTF_8.name());
    }


    /***
     * 渠道非标准json中的"'",主要是对苹果传过来的参数做过滤操作
     * **/
    public static String jsonStringRelace(String JsonString) {
        return JsonString.replace("\"{", "{").replace("}\"", "}").replace("\\", "");
    }

    /**
     * 把一个map拼接成一个get请求查询传
     *
     * @param data       map里面只应有业务查询参数,不应该包含sign和code
     * @param needEncode 是否使用URLEncode编码,默认为ture
     **/
    public static String toQueryString(Map<?, ?> data, boolean needEncode) throws UnsupportedEncodingException {
        String[] keys = data.keySet().toArray(new String[data.size()]);
        Arrays.sort(keys);
        StringBuffer queryString = new StringBuffer();
        for (String key : keys) {
            queryString.append(key + "=");
            if (needEncode) {
                queryString.append(URLEncoder.encode((String) data.get(key), StandardCharsets.UTF_8.name()) + "&");
            } else {
                queryString.append(data.get(key) + "&");
            }
        }
        if (queryString.length() > 0) {
            queryString.deleteCharAt(queryString.length() - 1);
        }
        return queryString.toString();
    }


    /**
     * @param obj
     * @param keepNull 如果keepNull=ture,则返回的obj中的空属性默认为'',否则将被过滤
     * @throws IllegalAccessException
     * @author Administrator
     **/
    public static Map<String, String> beanToMapWithString(Object obj, boolean keepNull) throws IllegalAccessException {
        if (obj == null) throw new IllegalAccessException("参数不能为空");
        Map<String, String> map = new HashMap<String, String>();
        //集合处理
        if (obj instanceof Collection) {
            Collection<?> collections = (Collection<?>) obj;
            map.put(API_MAP_SIZE, collections.size() + "");
            map.put(API_MAP_TYPE, API_REQ_TYPE_COLLECTION);
            return map;
        }
        ;
        //map 处理
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, ?> subMap = (Map<String, ?>) obj;
            subMap.keySet().forEach(s -> {
                Object o = subMap.get(s);
                if (o instanceof Number || o instanceof String) {
                    map.put(s, o.toString());
                }
            });
            if (map.keySet() != null && map.keySet().size() == 0) {
                map.put(API_MAP_SIZE, "0");
                map.put(API_MAP_TYPE, API_REQ_TYPE_MAP);
                return map;
            }
            return map;
        }
        //数字
        if (obj instanceof Number) {
            map.put(API_MAP_SIZE, 1 + "");
            map.put(API_MAP_TYPE, API_REQ_TYPE_NUMBER);
        }
        if (obj instanceof String) {
            map.put(API_MAP_SIZE, 1 + "");
            map.put(API_MAP_TYPE, API_REQ_TYPE_STRING);
        }
        //其他对象
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor property : propertyDescriptors) {
                String key = property.getName();
                // 过滤class属性
                if (!key.equals("class")) {
                    // 得到property对应的getter方法
                    Method getter = property.getReadMethod();
                    Object value = getter.invoke(obj);
                    if (value != null) {
                        if (value instanceof Number) {
                            map.put(key, value.toString());
                        }
                        if (value instanceof String) {
                            map.put(key, value.toString());
                        }
                    } else if (value == null && keepNull) {
                        map.put(key, "");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("javaBean转Map异常", e);
            throw new IllegalArgumentException(e);
        }
        return map;
    }

    //首字母转小写
    public static String toLowerCaseFirstOne(String s) {
        if (Character.isLowerCase(s.charAt(0)))
            return s;
        else
            return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
    }


    //首字母转大写
    public static String toUpperCaseFirstOne(String s) {
        if (Character.isUpperCase(s.charAt(0)))
            return s;
        else
            return (new StringBuilder()).append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).toString();
    }

    public static JavaType getCollectionType(ObjectMapper mapper, Class<?> collectionClass, Class<?>... elementClasses) {
        return mapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }
}
