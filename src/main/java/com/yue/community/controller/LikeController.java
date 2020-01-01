package com.yue.community.controller;

import com.yue.community.entity.User;
import com.yue.community.service.LikeService;
import com.yue.community.util.CommunityUtil;
import com.yue.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;


    // 处理异步请求
    @RequestMapping(path = "/like", method = RequestMethod.POST)
    @ResponseBody // 异步请求
    public String like(int entityType, int entityId, int entityUserId){
        // 获取当前用户
        User user = hostHolder.getUser();

        // 实现点赞
        likeService.like(user.getId(), entityType, entityId, entityUserId);
        // 点赞数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        // 点赞状态
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);

        // 将likeCount和likeStatus封装，然后传入页面
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        return CommunityUtil.getJSONString(0, null, map); // 正常状态返回0

    }

}
