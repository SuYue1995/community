package com.yue.community.controller.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component //声明注解，让Spring容器管理
public class AlphaInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AlphaInterceptor.class);
    //在Controller之前执行拦截
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        logger.debug("preHandle: "+ handler.toString()); //为了查看程序执行到该方法，打日志，日志为调试作用，debug级别即可
        return true; //return false，取消请求，不再往下执行，controller不会被执行，所以通常返回true；
    }

    //在调用完Controller之后执行拦截。主要的请求的逻辑已经处理完，下一步去模板引擎，给页面返回需要渲染的内容。参数ModelAndView可以对传递的数据进行获取、修改
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        logger.debug("postHandle：" + handler.toString());
    }

    //在程序最后执行，模板引擎执行完之后再执行
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception { //Exception对象提供异常信息
        logger.debug("afterCompletion：" + handler.toString());
    }
}
