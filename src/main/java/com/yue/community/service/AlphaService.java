package com.yue.community.service;

import com.yue.community.dao.AlphaDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service // 希望该Bean被容器管理，业务组件由@Service注解
//@Scope("prototype") // 作用范围，多个实例
public class AlphaService {

    @Autowired
    private AlphaDao alphaDao; //处理查询业务时调用

    public AlphaService(){
        System.out.println("实例化AlphaService");
    }

    @PostConstruct //该方法在构造器之后调用。初始化方法通常是在构造器之后调用，用来初始化某些数据
    public void init(){
        System.out.println("初始化AlphaService");
    }

    @PreDestroy // 在销毁对象之前调用，释放某些资源。销毁之后无法调用
    public void destroy(){
        System.out.println("销毁AlphaService");
    }

    public String find(){
        return alphaDao.select();
    }
}
