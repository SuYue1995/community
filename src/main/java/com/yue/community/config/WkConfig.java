package com.yue.community.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

@Configuration // 带有@Configuration注解，服务启动时，Spring会加载、实例化该类
public class WkConfig {

    private static final Logger logger = LoggerFactory.getLogger(WkConfig.class);

    // 注入命令
    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @PostConstruct // 实例化
    public void init(){
        // 创建wk图片目录
        File file = new File(wkImageStorage);
        if (!file.exists()){
            file.mkdir();
            logger.info("创建WK图片目录：" + wkImageStorage);
        }
    }
}
