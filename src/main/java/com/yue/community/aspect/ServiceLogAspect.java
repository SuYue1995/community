package com.yue.community.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

//@Component
//@Aspect
public class ServiceLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);

    // 声明切点
    @Pointcut("execution(* com.yue.community.service.*.*(..))")
    public void pointcut(){

    }

    // 用前置通知在一开头织入程序
    @Before("pointcut()")
    public void before(JoinPoint joinPoint){
        // 日志格式：用户[1.2.3.4], 在[xxx],访问了[com.yue.community.service.xxx()].

        // 需要获取用户的ip，此处无法简单的声明一个request对象，可利用工具类RequestContextHolder获取
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes(); // 将工具类的静态方法的返回值转换为其子类型

        // 通过attributes得到request对象
        if (attributes == null){ // 如果service调用是特殊调用，而非常规调用，则不记录日志
            return;
        }
        HttpServletRequest request = attributes.getRequest(); // 之前没有生产者与消费者，当前aop拦截的是所有service，所有对service的访问都是通过controller访问的。但现在Consumer也可调用service，而不是controller，所以就没有request，导致空指针异常。
        String ip = request.getRemoteHost();
        // 获取当前时间
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        // 获取当前类的方法。添加参数JoinPoint
        String target = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName(); //类名+方法名
        logger.info(String.format("用户[%s], 在[%s], 访问了[%s].", ip, now, target));
    }
}
