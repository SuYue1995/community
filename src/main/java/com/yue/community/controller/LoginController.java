package com.yue.community.controller;

import com.yue.community.entity.User;
import com.yue.community.service.UserService;
import com.yue.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@Controller
public class LoginController implements CommunityConstant {

    @Autowired
    private UserService userService;

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
}
