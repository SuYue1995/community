package com.yue.community.controller;

import com.yue.community.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
public class DataController {

    @Autowired
    private DataService dataService;

    // 统计页面
    // getUV方法可以跳转到该方法，同一个请求内，在处理过程中，请求类型不变，所以要求该方法支持POST请求，才能够被转发
    @RequestMapping(path = "/data", method = {RequestMethod.GET, RequestMethod.POST})
    public String getDataPage(){
        return "/site/admin/data";
    }

    // 处理统计网站UV的请求
    @RequestMapping(path = "/data/uv", method = RequestMethod.POST)
    public String getUV(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model){ //@DateTimeFormat(pattern = "yyyy-MM-dd")指定页面出入的日期格式
        long uv = dataService.calculateUV(start, end);
        model.addAttribute("uvResult", uv);
        // 统计完成后返回该页面，日期默认显示为之前输入日期，所以再传回给页面
        model.addAttribute("uvStartDate", start);
        model.addAttribute("uvEndDate", end);
//        return "/site/admin/data"; // 返回模板，返回给DispatcherServlet，它得到模板后，让模板做后续处理
        return "forward:/data"; // forward转发，声明当前的方法只能把请求处理一般，还需要另外方法继续处理请求，另外的方法是一个平级的处理请求的方法，不是模板，另外方法中的逻辑可以被复用。
    }

    // 统计活跃用户
    @RequestMapping(path = "/data/dau", method = RequestMethod.POST)
    public String getDAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model){ //@DateTimeFormat(pattern = "yyyy-MM-dd")指定页面出入的日期格式
        long dau = dataService.calculateDAU(start, end);
        model.addAttribute("dauResult", dau);
        // 统计完成后返回该页面，日期默认显示为之前输入日期，所以再传回给页面
        model.addAttribute("dauStartDate", start);
        model.addAttribute("dauEndDate", end);
        return "forward:/data"; // forward转发，声明当前的方法只能把请求处理一般，还需要另外方法继续处理请求，另外的方法是一个平级的处理请求的方法，不是模板，另外方法中的逻辑可以被复用。
    }
}
