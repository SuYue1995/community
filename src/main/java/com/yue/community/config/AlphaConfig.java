package com.yue.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;

@Configuration //非入口的普通配置类用@Configuration注解
public class AlphaConfig {

    @Bean //添加@Bean注解在方法之前,定义第三方Bean。该方法返回的对象将被装配到容器里
    public  SimpleDateFormat simpleDateFormat (){ //返回类型为SimpleDateFormat，方法名simpleDateFormat为Bean的名字
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //实例化SimpleDateFormat，并制定格式
    }
}
