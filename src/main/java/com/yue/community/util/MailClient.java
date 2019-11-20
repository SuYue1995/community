package com.yue.community.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Component //添加Component注解，统一由Spring容器进行管理。Component是一个通用的bean，每一个层次都可用
public class MailClient {

    private static final Logger looger = LoggerFactory.getLogger(MailClient.class); //声明一个logger，记录重要的日志

    @Autowired //发送邮件功能的核心组件JavaMailSender也是由Spring容器统一管理，直接注入到当前的bean中即可使用
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")//把username注入到bean中，免去每次调用
    private String from; //邮件发送方

    public void sendMail(String to, String subject, String content){ //邮件接收方，标题，内容
        try {
            MimeMessage message = mailSender.createMimeMessage();//创建MimeMessage，为空模板，用helper构建更详细的内容
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setText(content, true); //如果没有第二个参数，默认content为文本内容，设置html为true，则支持html内容
            mailSender.send(helper.getMimeMessage());
        } catch (MessagingException e) { //如果存在错误异常，则记录日志
            looger.error("发送邮件失败：" + e.getMessage());
        }
    }
}
