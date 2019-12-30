package com.yue.community.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

//@Component // 声明为不属于特定某一层的bean，让容器管理
//@Aspect // 声明为aspect组件
public class AlphaAspect {

    // 定义两个内容：1.切点（织入位置）
    // 通过注解定义切点,()括号中表达式来描述需要处理的目标bean。
    @Pointcut("execution(* com.yue.community.service.*.*(..))") // execution为固定的关键字，第一个* 表示任何返回值，*.*: service包下的所有业务组件中的所有方法，(..):所有参数
    public void pointcut(){

    }

    // 定义：2. 通知，利用通知明确解决问题
    // 分类：连接点开始、连接点结束、返回数据后、抛异常后、连接点前后同时。五种通知通过五种注解实现
    @Before("pointcut()")
    public void before(){
        System.out.println("before");
    }

    @After("pointcut()")
    public void after(){
        System.out.println("after");
    }

    @AfterReturning("pointcut()")
    public void afterReturning(){
        System.out.println("afterReturning");
    }

    @AfterThrowing("pointcut()")
    public void afterThrowing(){
        System.out.println("afterThrowing");
    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{ // 参数为连接点，织入的位置
        System.out.println("around before");
        Object obj = joinPoint.proceed(); // 调需要处理的目标组件的方法。可能有返回值。此处调用原始对象
        System.out.println("around after");
        return obj;
        // 程序执行时执行代理对象，以上逻辑被织入到代理对象里，用来代替原始对象
    }

}
