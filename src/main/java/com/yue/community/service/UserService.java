package com.yue.community.service;

import com.yue.community.dao.UserMapper;
import com.yue.community.entity.User;
import com.yue.community.util.CommunityConstant;
import com.yue.community.util.CommunityUtil;
import com.yue.community.util.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient; //注入邮箱客户端

    @Autowired
    private TemplateEngine templateEngine;

    //邮件中包含激活码，激活码包含域名和项目名，所以将配置文件中的域名和项目名注入进来
    @Value("${community.path.domain}") //注入固定的值，而不是bean，用@Value注解
    private String domain; //域名

    @Value("${server.servlet.context-path}")
    private String contextPath; //项目名

    public User findUserById(int id){
        return userMapper.selectById(id);
    }

    //开始编写业务，写一个共有方法，方便复用
    public Map<String, Object> register(User user){
        Map<String, Object> map = new HashMap<>();

        //先对传入的user进行判断处理
        //空值处理
        if (user == null){
            throw new IllegalArgumentException("参数不能为空！");
        }
        //username为空
        if (StringUtils.isBlank(user.getUsername())){
            //不是程序错误，而是业务逻辑错误，所以不是抛异常，而是给出提示信息
            map.put("usernameMsg","账号不能为空！");
            return map;
        }
        //password为空
        if (StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg","密码不能为空！");
            return map;
        }
        //email为空
        if (StringUtils.isBlank(user.getEmail())){
            map.put("emailMsg","邮箱不能为空！");
            return map;
        }
        //判断空值之后，还需要判断是否已存在
        //验证账号
        User u = userMapper.selectByName(user.getUsername());
        if (u != null){
            map.put("usernameMsg", "该账号已存在！");
            return map;
        }
        //验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null){
            map.put("emailMsg", "该邮箱已被注册！");
            return map;
        }

        //注册用户
        //生成salt，用于密码加密
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));//5位随机字符串即可
        //密码加密
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));
        //设置其他字段
        user.setType(0); //注册用户默认为普通用户
        user.setStatus(0);//注册用户默认未激活
        user.setActivationCode(CommunityUtil.generateUUID());//设置随机字符串为激活码
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));//%d占位符，0-1000随机数
        user.setCreateTime(new Date());
        //添加进数据库
        userMapper.insertUser(user);

        //发送激活邮件，html邮件可带链接
        Context context = new Context();//实例化Thymeleaf包下的对象Context，用于携带变量
        context.setVariable("email", user.getEmail());
        //激活路径 http://127.0.0.1:8000/community/activation/101/code 101(用户账号) code(激活码)
        //动态拼接激活路径
        String url = domain+contextPath+"/activation/"+ user.getId() + "/" + user.getActivationCode(); //"/activation"为功能访问名，可以写定。
        //注册时，用户传进来的user对象中没有userId，调用insertUser语句之后，Mybatis自动获取自动生成的Id，并进行回填
        //mybatis.configuration.useGeneratedKeys=true 自动生成userId，user-mapper中设置了对应关系，所以user.getId()就有值了
        context.setVariable("url", url);
        //用模板引擎生成邮件内容
        String content = templateEngine.process("/mail/activation",context);
        mailClient.sendMail(user.getEmail(),"激活账号", content);

        return map; //将异常信息返回给页面显示，如果map没空，则没有问题
    }

    //激活账户业务方法
    public int activation(int userId, String code){ //返回激活状态int。传入参数 userId 和 激活码 code
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1){ //初始化Status=0，如果为1则表示已激活
            return ACTIVATIOM_REPEAT;
        }else if (user.getActivationCode().equals(code)){ //激活码匹配，激活成功
            userMapper.updateStatus(userId,1);//更新Status为1
            return ACTIVATIOM_SUCCESS;
        }else {
            return ACTIVATION_FAILURE;
        }
    }
}
