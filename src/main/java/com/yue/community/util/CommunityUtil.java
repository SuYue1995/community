package com.yue.community.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.UUID;

public class CommunityUtil {

    //生成随机字符串
    //UUID工具可生成随机字符串，简单封装即可
    public static String generateUUID(){
        return UUID.randomUUID().toString().replaceAll("-","");//UUID生成的字符串由字母数字和横线构成，不想要横线，用空字符串替换掉
    }

    //MD5加密，用MD5算法对密码进行加密，然后存进数据库
    //hello -> abc123def456 MD5同一字符串相同加密结果，常用简单字符串的加密容易被盗取
    //hello + 3e4f8(随机字符串) -> abc123def456abc 密码后面加随机字符串（user中的salt字段），再进行加密，提高安全性
    public static String md5(String key){
        if (StringUtils.isBlank(key)){ //用导入的apache.commons.lang3字符串判空方法对key进行判空
            return null;
        }
        return DigestUtils.md5DigestAsHex(key.getBytes());//用Spring自带的DigestUtils中的md5，将字节转化成16进制
    }

}
