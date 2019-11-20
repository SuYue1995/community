package com.yue.community;

import com.yue.community.util.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)

public class MailTests {
    @Autowired
    private MailClient mailClient;

    //Thymeleaf模板引擎类被spring容器管理起来，直接注入调用即可
    @Autowired
    private TemplateEngine templateEngine;

    @Test
    public void testTextMail(){
        mailClient.sendMail("343515828@qq.com","TEST","Welcome.");
    }

    @Test
    public void testHtmlMail(){
        Context context = new Context(); //Thymeleaf包中的Context。利用context给Thymeleaf模板传递参数
        context.setVariable("username","suyue");
        //调用模板引擎生成动态网页
        String content = templateEngine.process("/mail/demo",context); //模板路径和参数，生成动态网页（其实就是一个字符串）
        System.out.println(content);
        mailClient.sendMail("343515828@qq.com","HTML", content);

    }
}
