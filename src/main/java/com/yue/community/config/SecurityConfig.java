package com.yue.community.config;

import com.yue.community.util.CommunityConstant;
import com.yue.community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {

    @Override
    public void configure(WebSecurity web) throws Exception {
        // 忽略静态资源的拦截
        web.ignoring().antMatchers("/resources/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 授权
        http.authorizeRequests()
                .antMatchers( // 只有登录才能访问以下路径
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/unfollow"
                )
                .hasAnyAuthority( // 有以下任一权限即可访问以上路径
                        AUTHORITY_USER,
                        AUTHORITY_ADMIN,
                        AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                )
                .hasAnyAuthority(
                        AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/delete",
                        "/data/**",
                        "/actuator/**"
                ).hasAnyAuthority(
                        AUTHORITY_ADMIN
                )
                .anyRequest().permitAll() // 其余的路径统统允许访问
                .and().csrf().disable(); // 暂不启用csrf检查

        // 权限不够时的处理
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() { // 没有登录时如何处理。
                    @Override
                    public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {
                        // 考虑请求的方式：如果为普通请求，重定向到登录页面；如果为异步请求，拼接json字符串，返回提示
                        // 通过请求头部的一个值判断请求方式
                        String xRequestedWith = httpServletRequest.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)){ // 异步请求
                            // 给浏览器输出响应，响应json字符串
                            httpServletResponse.setContentType("application/plain;charset=utf-8"); // 声明返回的数据类型，普通json格式的字符串
                            PrintWriter writer = httpServletResponse.getWriter(); // 字符流
                            writer.write(CommunityUtil.getJSONString(403, "你还没有登录哦！"));
                        }else { // 同步请求
                            // 重定向到登录页面
                            httpServletResponse.sendRedirect(httpServletRequest.getContextPath() + "/login");
                        }
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() { // 登录后权限不足如何处理。因为存在异步请求期待返回json，accessDeniedPage只能返回html，不适用，所以用Handler组件处理，组件逻辑可自定义
                    @Override
                    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedWith = httpServletRequest.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)){ // 异步请求
                            // 给浏览器输出响应，响应json字符串
                            httpServletResponse.setContentType("application/plain;charset=utf-8"); // 声明返回的数据类型，普通json格式的字符串
                            PrintWriter writer = httpServletResponse.getWriter(); // 字符流
                            writer.write(CommunityUtil.getJSONString(403, "你没有访问此功能的权限！"));
                        }else { // 同步请求
                            // 重定向到没有权限的页面
                            httpServletResponse.sendRedirect(httpServletRequest.getContextPath() + "/denied");
                        }
                    }
                });

        // Security底层默认自动拦截/logout请求，进行退出处理。Security底层通过filter管理权限，代码的执行在DispatcherServlet之前，在controller之前。
        // filter提前拦截到logout，进行退出处理之后，程序不再往后面执行，自定义的logout不会执行
        // 覆盖它默认的逻辑才能执行自己的退出代码
        http.logout().logoutUrl("/securitylogout"); // logoutUrl默认为"/logout"，修改为其他不处理的路径，即可绕过
    }
}
