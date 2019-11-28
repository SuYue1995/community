package com.yue.community.util;

import com.yue.community.entity.User;
import org.springframework.stereotype.Component;

/**
 * 持有用户的信息，用于代替session对象，线程隔离
 */
@Component
public class HostHolder {

    private ThreadLocal<User> users = new ThreadLocal<>();//泛型指定里面存的是User对象，里面存储每个线程对应的user

    public void setUser(User user){ //方便外界传入user
        users.set(user);
    }
    public User getUser(){
        return users.get();
    }

    //请求结束时，清理ThreadLocal中的user，防止占用过多的内存
    public void clear(){
        users.remove();
    }
}
