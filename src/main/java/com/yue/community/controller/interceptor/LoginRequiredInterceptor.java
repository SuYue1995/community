package com.yue.community.controller.interceptor;

import com.yue.community.annotation.LoginRequired;
import com.yue.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHolder hostHolder;

    //在请求的最初判断是否登录
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断拦截的目标是否是方法，可能会拦截到静态资源等其他
        if (handler instanceof HandlerMethod){ //HandlerMethod是SpringMVC提供的一个类型，如果拦截到的是方法，则handler对象是HandlerMethod类型
            //转型，方便调用方法
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            //直接获取到拦截到的Method对象
            Method method = handlerMethod.getMethod();
            //尝试从Method对象上按照指定类型去取注解
            LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
            if (loginRequired != null && hostHolder.getUser()==null){ //loginRequired不为空说明该方法需要登录才能访问，getUser==null，未登录
                //未登录，则强制重定向到登录页面
                //利用response对象进行重定向。 因为preHandle是接口声明的，返回值为布尔值，不能return模板，所以需要response进行重定向
                response.sendRedirect(request.getContextPath() + "/login");//controller中return redirect 底层也是这么实现的。从request直接取到项目路径
                return false; //拒绝后续的请求
            }
        }
        return true;
    }
}
