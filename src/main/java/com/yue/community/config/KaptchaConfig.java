package com.yue.community.config;

import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KaptchaConfig{

    @Bean //声明一个bean，服务启动时，自动装配到Spring容器里，被容器所管理。
    // kaptcha核心接口为Producer，实例化该接口。通过容器就可以得到Producer实例，可调用其两个方法，创建验证码文字和图片。
    public Producer kaptchaProducer(){
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha(); //实例化producer的实现类
        //向defaultKaptcha中传入一些参数配置，将这些参数封装到Config对象中
        Properties properties = new Properties();
        properties.setProperty("kaptcha.image.width","100");
        properties.setProperty("kaptcha.image.height","40");
        properties.setProperty("kaptcha.textproducer.font.size","32");
        properties.setProperty("kaptcha.textproducer.font.color","0,0,0");
        properties.setProperty("kaptcha.textproducer.char.string","0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");//随机字符的范围
        properties.setProperty("kaptcha.textproducer.char.length","4");//随机字符长度限定
        properties.setProperty("kaptcha.noise.impl","com.google.code.kaptcha.impl.NoNoise");//采用的干扰噪声类，3d,拉伸，阴影等，防止机器人暴力破解。因为自带噪声，所以设置为NoNoise
        Config config = new Config(properties); //Config对象的参数为properties对象，里面存的key-value,其实就是一个map，map里面存的参数
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
}
