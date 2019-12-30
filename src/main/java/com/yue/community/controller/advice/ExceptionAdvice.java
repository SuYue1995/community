package com.yue.community.controller.advice;


import com.yue.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@ControllerAdvice(annotations = Controller.class) //如果不设定annotations组件会扫描所有的bean。设置后，只扫描带有@controller注解的bean
public class ExceptionAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    // 处理所有的错误情况
    @ExceptionHandler({Exception.class}) //大括号中可写多个数据，设定需要处理的异常。Exception是所有异常的父类，因此可处理所有异常
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException { //Exception:controller发生异常时会传过来。基本上这三个参数可解决绝大多数问题，更多参数参考手册
        logger.error("服务器发生异常：" + e.getMessage()); // 日志中记录异常的概括
        //记录详细的异常信息
        for (StackTraceElement element : e.getStackTrace()){ //每一个element 记录一条异常信息
            logger.error(element.toString());
        }

        // 记录完日志后，给浏览器一个响应，重定向到错误页面
        // 浏览器访问服务器可能是普通的请求（返回网页），也有可能是异步请求（返回json）
        // 判断是普通请求还是异步请求
        String xRequestedWith = request.getHeader("x-requested-with");
        if ("XMLHttpRequest".equals(xRequestedWith)){ //异步请求
            response.setContentType("application/plain; charset=utf-8"); // 返回普通字符串，字符串可以是json格式，浏览器接收到后，需要人为地用$.parseJson()转换为js对象.声明字符集，支持中文
            // 获取输出流，向外输出字符串
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(1, "服务器异常！"));
        }else { //普通请求，重定向到错误页面
            response.sendRedirect(request.getContextPath() + "/error");
        }
    }
}
