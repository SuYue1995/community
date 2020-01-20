package com.yue.community.service;

import com.yue.community.dao.LoginTicketMapper;
import com.yue.community.dao.UserMapper;
import com.yue.community.entity.LoginTicket;
import com.yue.community.entity.User;
import com.yue.community.util.CommunityConstant;
import com.yue.community.util.CommunityUtil;
import com.yue.community.util.MailClient;
import com.yue.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient; //注入邮箱客户端

    @Autowired
    private TemplateEngine templateEngine;

    // * 重构，不再使用loginTicketMapper这个bean
/*    @Autowired
    private LoginTicketMapper loginTicketMapper;*/

    @Autowired
    private RedisTemplate redisTemplate;

    //邮件中包含激活码，激活码包含域名和项目名，所以将配置文件中的域名和项目名注入进来
    @Value("${community.path.domain}") //注入固定的值，而不是bean，用@Value注解
    private String domain; //域名

    @Value("${server.servlet.context-path}")
    private String contextPath; //项目名

    // * 重构 用redis缓存用户信息
    public User findUserById(int id){
//        return userMapper.selectById(id);
        // 先从缓存Cache中查询用户
        User user = getCache(id);
        // 如果缓存中没有该用户，则初始化
        if (user == null){
            user = initCache(id);
        }
        return user;
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
            clearCache(userId); // * 重构 redis缓存用户信息，更改数据时，删除缓存
            return ACTIVATIOM_SUCCESS;
        }else {
            return ACTIVATION_FAILURE;
        }
    }

    //登录业务
    public  Map<String, Object> login(String username, String password, long expiredSeconds){ //map封装多种情况的返回结果，因为登录失败有多种原因
        Map<String, Object> map = new HashMap<>();
        //空值判断处理
        if (StringUtils.isBlank(username)){
            map.put("usernameMsg", "账号不能为空");
            return map;
        }
        if (StringUtils.isBlank(password)){
            map.put("passwordMsg","密码不能为空");
            return map;
        }

        //如果账号密码都不为空，进行合法性验证，验证账号
        //验证账号是否存在
        User user = userMapper.selectByName(username);
        if (user == null){
            map.put("usernameMsg","该账号不存在");
            return map;
        }
        //验证账号是否激活
        if (user.getStatus() == 0){
            map.put("usernameMsg","该账号未激活");
            return map;
        }

        //验证密码
        //将传入的明文密码按照同样的md5进行加密，再进行比较
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)){
            map.put("passwordMsg","密码不正确");
            return map;
        }

        //登录成功，生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID()); //UUID是不重复的
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        //loginTicketMapper.insertLoginTicket(loginTicket);
        // * 重构 将ticket存在redis中
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey, loginTicket); // redis会把对象loginTicket序列化为JSON字符串，然后保存

        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    //退出业务
    public void logout(String ticket){
//        loginTicketMapper.updateStatus(ticket,1);//1表示无效
        // * 重构
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1); // 将状态改为1，为无效
        redisTemplate.opsForValue().set(redisKey, loginTicket);


    }

    //查询凭证ticket
    public LoginTicket findLoginTicket(String ticket){
//        return loginTicketMapper.selectByTicket(ticket);
        // * 重构
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }

    // * 重构 redis存储用户信息
    //更新用户的头像路径，返回修改行数
    public int updateHeader(int userId, String headerUrl){
//        return userMapper.updateHeader(userId,headerUrl);
        int rows = userMapper.updateHeader(userId,headerUrl);
        clearCache(userId); // 更改数据，清楚缓存
        return rows;
    }

    //修改密码
    public Map<String, Object> updatePassword(int userId, String oldPassword, String newPassword){
        Map<String, Object> map = new HashMap<>();
        User user = userMapper.selectById(userId);

        //判断空值
        if (StringUtils.isBlank(oldPassword)){
            map.put("oldPasswordMsg","原密码不能为空");
            return map;
        }
        if (StringUtils.isBlank(newPassword)){
            map.put("newPasswordMsg","新密码不能为空");
            return map;
        }

        //验证原密码
        oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
        if (!user.getPassword().equals(oldPassword)){
            map.put("oldPasswordMsg","原密码不正确");
            return map;
        }

        //验证新密码是否和原密码相同
        newPassword = CommunityUtil.md5(newPassword + user.getSalt());
        if (user.getPassword().equals(newPassword)){
            map.put("newPasswordMsg", "新密码不能和原密码相同");
            return map;
        }

        userMapper.updatePassword(userId, newPassword);
        return map;
    }

    // 通过用户名查询用户
    public User findUserByName(String username){
        return userMapper.selectByName(username);
    }

    // 优化登录模块，Redis缓存用户信息
    // UserService中内部调用，所以private即可
    // 1. 优先从缓存中取值
    private User getCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }
    // 2. 取到直接返回，取不到时初始化缓存数据
    private User initCache(int userId){
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    // 3. 数据变更时清除缓存数据
    private void clearCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }
}
