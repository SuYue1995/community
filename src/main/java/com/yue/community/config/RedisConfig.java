package com.yue.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory){
        // redisTemplate 要访问数据库，需要连接数据库，通过连接工厂进行连接
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory); //把工厂设置给template，即具备了访问数据库的能力

        // 配置序列化的方式，即数据转换的方式
        // 设置key的序列化方式
        template.setKeySerializer(RedisSerializer.string()); // 括号中为返回字符串的序列化器
        // 设置value的序列化方式
        template.setValueSerializer(RedisSerializer.json()); // value有多种类型，json的格式是结构化的，容易转换

        // hash也是key-value，所以需要单独设置
        // 设置hash的key的序列化方式
        template.setHashKeySerializer(RedisSerializer.string());
        // 设置hash的value的序列化方式
        template.setHashValueSerializer(RedisSerializer.json());

        // 使设置生效
        template.afterPropertiesSet();
        return template;
    }
}
