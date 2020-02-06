package com.yue.community.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling //添加该注解才能启用scheduler
@EnableAsync
public class ThreadPoolConfig {

}
