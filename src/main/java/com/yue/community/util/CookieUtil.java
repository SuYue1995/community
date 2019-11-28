package com.yue.community.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class CookieUtil {
    public static String getValue(HttpServletRequest request, String name){
        if (request == null || name == null){
            throw new IllegalArgumentException("参数为空！");
        }
        Cookie[] cookies = request.getCookies(); //得到数组，包含所有的cookie对象
        if (cookies != null){
            for (Cookie cookie:cookies){ //遍历所有cookie，寻找key为参数name的cookie，然后返回cookie的value值
                if (cookie.getName().equals(name)){
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
