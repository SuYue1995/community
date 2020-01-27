package com.yue.community.config;

import com.yue.community.controller.interceptor.AlphaInterceptor;
import com.yue.community.controller.interceptor.LoginRequiredInterceptor;
import com.yue.community.controller.interceptor.LoginTicketInterceptor;
import com.yue.community.controller.interceptor.MessageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration //声明配置类    //之前的配置类用于声明一个第三方的bean，拦截器不太一样，不是简单的装配一个bean，而是实现一个接口
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AlphaInterceptor alphaInterceptor; //注入拦截器，实现WebMvcConfigurer接口，在某个方法里注册拦截器

    @Autowired
    private LoginTicketInterceptor loginTicketInterceptor;//第一步：注入拦截器

    @Autowired
    private LoginRequiredInterceptor loginRequiredInterceptor;

    @Autowired
    private MessageInterceptor messageInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) { //利用Spring传进来的对象registry去注册拦截器
        registry.addInterceptor(alphaInterceptor) //直接这么写，拦截所有路径
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg")//排除路径，对这些路径不进行拦截。静态资源css，js等无需拦截。"/**"表示static目录下所有文件夹。http://127.0.0.1:8000/community/css/letter.css访问静态资源路径
                .addPathPatterns("/register", "/login");//明确需要拦截的路径。也可以使用通配符统一添加，例如"/user/**" Controller user 下的方法

        //第二步：注册拦截器
        registry.addInterceptor(loginTicketInterceptor) //直接这么写，拦截所有路径
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");//其余所有页面都进行拦截处理，不用addPathPatterns();

        registry.addInterceptor(loginRequiredInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");//不处理静态资源，其他动态资源都处理

        registry.addInterceptor(messageInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");//不处理静态资源，其他动态资源都处理
    }
}
