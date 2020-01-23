package com.yue.community.controller;

import com.yue.community.entity.Event;
import com.yue.community.entity.Page;
import com.yue.community.entity.User;
import com.yue.community.event.EventProducer;
import com.yue.community.service.FollowService;
import com.yue.community.service.UserService;
import com.yue.community.util.CommunityConstant;
import com.yue.community.util.CommunityUtil;
import com.yue.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
public class FollowController implements CommunityConstant{

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private EventProducer eventProducer;

    // 关注，异步请求，当前页面不刷新
    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody // 异步请求
    public String follow(int entityType, int entityId){

        User user = hostHolder.getUser();
        followService.follow(user.getId(), entityType, entityId);

        // 触发关注事件
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId); // 因为目前业务只能关注用户，所以entityUserId=entityId
        eventProducer.fireEvent(event);

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


    // 获取关注列表
    @RequestMapping(path = "/followees/{userId}", method = RequestMethod.GET)
    public String getFollowees(@PathVariable("userId") int userId, Page page, Model model){
        User user = userService.findUserById(userId);
        if (user == null){
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user", user);

        page.setLimit(5);
        page.setPath("/followees/" + userId);
        page.setRows((int) followService.findFolloweeCount(userId, ENTITY_TYPE_USER));

        List<Map<String, Object>> userList = followService.findFollowees(userId, page.getOffset(), page.getLimit());
        if (userList != null){
            for (Map<String, Object> map : userList){
                User u =(User) map.get("user");
                map.put("hasFollowed",hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", userList);
        return "/site/followee";
    }

    // 获取粉丝列表
    @RequestMapping(path = "/followers/{userId}", method = RequestMethod.GET)
    public String getFollowers(@PathVariable("userId") int userId, Page page, Model model){
        User user = userService.findUserById(userId);
        if (user == null){
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user", user);

        page.setLimit(5);
        page.setPath("/followers/" + userId);
        page.setRows((int) followService.findFollowerCount(ENTITY_TYPE_USER, userId));

        List<Map<String, Object>> userList = followService.findFollowers(userId, page.getOffset(), page.getLimit());
        if (userList != null){
            for (Map<String, Object> map : userList){
                User u =(User) map.get("user");
                map.put("hasFollowed",hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", userList);
        return "/site/follower";
    }

    private boolean hasFollowed(int userId){
        if (hostHolder.getUser() == null){
            return false;
        }
        return followService.hasFollowed(hostHolder.getUser().getId(), CommunityConstant.ENTITY_TYPE_USER, userId);
    }

}
