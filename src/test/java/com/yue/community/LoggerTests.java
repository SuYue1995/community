package com.yue.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)

public class LoggerTests {
    //每一个类记录日志，单独为该类实例化一个Logger。为了便于所有的方法调用，通常设置为静态的static。
    // 实例化Logger用logback自带的工厂LoggerFactory。
    // 传入一个名字，为该Logger的名字，通常传入当前类。这样，不同的Logger在不同的类下就有一个区别，打印在日志上，也可得知Logger所在类。
    private static final Logger logger = LoggerFactory.getLogger(LoggerTests.class);

    @Test
    public void testLogger(){
        System.out.println(logger.getName());
        logger.debug("debug log");
        logger.info("info log");
        logger.warn("warn log");
        logger.error("error log");
    }
}
