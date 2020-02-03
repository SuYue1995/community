package com.yue.community.controller.interceptor;

import com.yue.community.entity.LoginTicket;
import com.yue.community.entity.User;
import com.yue.community.service.UserService;
import com.yue.community.util.CookieUtil;
import com.yue.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component //声明注解，让Spring容器管理
public class LoginTicketInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    //在请求开始时获取ticket然后用其查询登录用户，然后暂存用户信息。因为在请求当中，随时随地都有可能用到当前用户，所以一开始就获得用户数据
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //通过cookie获得ticket凭证
        //因为该方法是接口中定义好的，不能添加修改参数，所以不可使用@CookieValue注解。从request中可以取出cookie，用已经封装好的CookieUtil工具取到cookie值
        String ticket = CookieUtil.getValue(request,"ticket");
        if (ticket != null){ //登录可以得到，不登录为空
            //查询凭证，LoginTicket对象
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            //检查凭证是否有效，有效则暂存，无效则视为未登录
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())){ //查询到的loginTicket不为空，并且状态是0有效，并且超时时间晚于当前时间

                //根据登录凭证查询用户
                User user = userService.findUserById(loginTicket.getUserId());

                //查询到的user在模板，或者controller处理以及后续其他地方也可能会用到。为了后续使用，暂存user
                //在本次请求中持有用户
                //浏览器对服务器是多对一，服务器可以同时处理多个请求，是并发的。每个浏览器访问服务器，服务器创建独立线程处理请求，所以服务器处理请求时是多线程环境
                //所以暂存用户需要考虑多线程情况，如果简单存在容器或工具当中作为一个变量，在并发情况下可能产生冲突
                //把数据存在一个地方，让多线程并发访问没有问题，需要考虑线程隔离，即每个线程单独存一份，不互相干扰，使用ThreadLocal解决此问题
                //调用封装好的HostHolder工具去持有用户，将数据存放在当前线程的map里，请求处理完之前线程都在；请求处理完，服务器向浏览器做出相应之后，线程被销毁。
                hostHolder.setUser(user);

                // 构建用户认证的结果，并通过并通过存入SecurityContextHolder存入SecurityContext，便于Security进行授权
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        // 三个参数：principal：认证的主要信息，一般为user；credentials：证书，账号密码模式下为密码；authorities：权限
                        user, user.getPassword(), userService.getAuthorities(user.getId()));
                // 构造完认证结果，将其存储在SecurityContext里
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }
        }
        return true;
    }

    //在调用模板引擎之前，把user存入model中
    //postHandle在模板引擎之前调用，且有ModelAndView对象参数
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user!=null && modelAndView!=null){
            modelAndView.addObject("loginUser",user);
        }
    }

    //最后调用clear方法清理数据
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
        SecurityContextHolder.clearContext();
    }
}
