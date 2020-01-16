package com.yue.community.controller;

import com.yue.community.entity.User;
import com.yue.community.service.FollowService;
import com.yue.community.util.CommunityUtil;
import com.yue.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FollowController {

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    // 关注，异步请求，当前页面不刷新
    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody // 异步请求
    public String follow(int entityType, int entityId){

        User user = hostHolder.getUser();
        followService.follow(user.getId(), entityType, entityId);
        // 异步请求，给页面返回json
        return CommunityUtil.getJSONString(0, "已关注！");
    }

    // 取消关注
    @RequestMapping(path = "/unfollow", method = RequestMethod.POST)
    @ResponseBody // 异步请求
    public String unfollow(int entityType, int entityId){

        User user = hostHolder.getUser();
        followService.unfollow(user.getId(), entityType, entityId);
        // 异步请求，给页面返回json
        return CommunityUtil.getJSONString(0, "已取消关注！");
    }
}
