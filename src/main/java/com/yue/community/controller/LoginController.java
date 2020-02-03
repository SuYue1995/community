package com.yue.community.controller;

import com.google.code.kaptcha.Producer;
import com.yue.community.entity.User;
import com.yue.community.service.UserService;
import com.yue.community.util.CommunityConstant;
import com.yue.community.util.CommunityUtil;
import com.yue.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    //将application.properties中定义的项目名注入进来
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @RequestMapping(path = "/register", method = RequestMethod.GET)//声明访问路径
    public String getRegisterPage(){
        return "/site/register";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)//声明访问路径
    public String getLoginPage(){
        return "/site/login";
    }

    //一个方法来处理注册的请求，浏览器提交注册数据，所以为Post请求
    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user){ //String返回视图名；往model中存数据携带给模板；
        // User对象接受三个参数（只要页面传入值的时候，值和user的属性相匹配，SpringMVC自动将值注入给user对象中的属性）
        Map<String, Object> map = userService.register(user); //通过返回的map进一步确定如何向浏览器做出响应
        if (map == null || map.isEmpty()){ //注册成功，页面提示成功。跳转到第三方页面operate-result.html,需要两个参数：提示信息和最终跳转页面
            model.addAttribute("msg","注册成功，我们已经向您的邮箱发送了一封激活邮件，请尽快激活");
            model.addAttribute("target","/index");
            return "/site/operate-result";
        }
        else { //注册失败，在注册页面显示失败信息
            model.addAttribute("usernameMsg", map.get("usernameMsg")); //全部接受
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }

    //激活路径 http://127.0.0.1:8000/community/activation/101/code 101(用户账号) code(激活码)
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET) //利用路径携带了两个条件，返回激活结果，成功或失败，相当于查询行为，所以为GET请求
     public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code){ //@PathVariable注解从路径中取值
        int result = userService.activation(userId,code);
        if (result == ACTIVATIOM_SUCCESS){ //无论成功或失败，跳转到提示页面，然后成功跳转到登录页面，失败跳转到首页
            model.addAttribute("msg","激活成功，您的账号已可以正常使用");
            model.addAttribute("target","/login");
        }else if (result == ACTIVATIOM_REPEAT){
            model.addAttribute("msg","无效操作，该账号已被激活过");
            model.addAttribute("target","/index");
        }else {
            model.addAttribute("msg","激活失败，您提供的激活码无效");
            model.addAttribute("target","/index");
        }
        return "/site/operate-result";
    }


    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)//获取图片，获取数据的方法，所以使用GET
    //因为向浏览器输出的是特殊的数据，不是字符串、网页，是一张图片，需要用response对象手动向浏览器输出，所以返回void
    //生成完验证码以后，服务端需要记住，登录时再次访问服务器时，验证验证码是否正确，不能存在浏览器端，容易盗取（敏感信息）
    //存在服务器端，多个请求需要使用（跨请求）。这次请求，创建验证码，存在服务端，下次登录请求时，再使用该验证码。
    //跨请求，利用cookie或session，因为是敏感数据，用session更安全
    public void getKaptcha(HttpServletResponse response/*, HttpSession session*/){ // * 重构此模块，不在使用Session，用Redis
        //生成验证码 需要使用配置类，产生的bean。bean通过容器获取，并注入到当前的bean之中.(在前面注入Producer)
        String text = kaptchaProducer.createText();//根据配置生成字符串
        BufferedImage image = kaptchaProducer.createImage(text); //通过字符串生成图片

        //将验证码存入session，后续使用
        // session.setAttribute("kaptcha", text); // * 重构，以下将验证码临时存放在redis中

        // 验证码的归属
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(60); // 验证码的生存时间为60s
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        // 将验证码存入redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS); // （redisKey，验证码） 有效时间60s

        //将图片输出给浏览器
        response.setContentType("image/png"); //声明给浏览器返回数据的格式
        try {
            OutputStream os = response.getOutputStream(); //response给浏览器做响应，需要获取输出流，writer()字符流，outputStream()字节流，获取图片使用字节流
            ImageIO.write(image,"png", os);//向浏览器输出图片image,格式png，输出流os输出。
            //OutputStream不用关闭，response由SpringMVC维护，会自动关闭
        } catch (IOException e) {
            logger.error("响应验证码失败：" + e.getMessage());
        }
    }

    // * 重构此模块，从redis获取验证码，不再使用session
    @RequestMapping(path = "/login", method = RequestMethod.POST) // 和之前getLoginPage方法路径/login相同，请求方式method不同即可区分，可行。处理表单提交的数据，用post请求
    public String login(String username, String password, String code, boolean rememberme, Model model,//表单传入的参数: 账号，密码，验证码，记住我勾选。返回响应需要Model
                        /*HttpSession session,*/ HttpServletResponse response,//从session获取之前存入的验证码。如果登录成功，需要将ticket发放给客户端用cookie保存。
                        @CookieValue("kaptchaOwner") String kaptchaOwner){ // * 重构 添加cookie参数，获取redis的key
        //先判断验证码是否正确，直接在表现层无需业务层判断
        // String kaptcha = (String) session.getAttribute("kaptcha");
        String kaptcha = null;
        if (StringUtils.isNotBlank(kaptchaOwner)){ //  如果取不到参数，则返回登录页面，显示“验证码不正确”
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }
        // 从redis中获取kaptcha，需要key，key存储在cookie中，需要添加cookie参数从中取值
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)){ //任一为空或者不匹配，错误。不区分大小写
            model.addAttribute("codeMsg","验证码不正确");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            return "/site/login";
        }
        //检查账号密码，业务层处理
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECOND : DEFAULT_EXPIRED_SECOND; //调service方法所需的参数，超时的秒数
        Map<String, Object> map = userService.login(username,password,expiredSeconds);
        if (map.containsKey("ticket")){ //map中包含ticket即成功
            //服务端给客户端发送一个cookie，携带ticket
            Cookie cookie = new Cookie("ticket",map.get("ticket").toString());
            cookie.setPath(contextPath);//登陆之后，有效路径应该包括整个项目
            cookie.setMaxAge(expiredSeconds);//设置cookie有效时间
            response.addCookie(cookie);//把cookie发送给页面，在响应时发送给浏览器
            return "redirect:/index"; //登录成功，重定向到首页
        }else { //登录失败
            //将错误消息带回给登录页面
            model.addAttribute("usernameMsg",map.get("usernameMsg"));//如果不是username的问题，map.get得到null，页面展现时不会影响
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "/site/login"; //返回登录页面
        }
    }

    //处理退出请求
    @RequestMapping(path = "/logout", method = RequestMethod.GET)//不需要处理表单提交数据，所以用GET
    public String logout(@CookieValue("ticket") String ticket){ //退出时会自动传入cookie，利用@CookieValue注解得到cookie
        userService.logout(ticket);
        SecurityContextHolder.clearContext();
        return "redirect:/login"; //重定向到登录页面， 我们有两个/login路径，默认为GET请求路径
    }
}
