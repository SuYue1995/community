package com.yue.community.service;

import com.yue.community.dao.DiscussPostMapper;
import com.yue.community.entity.DiscussPost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiscussPostService {

    @Autowired
    private DiscussPostMapper discussPostMapper;

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit){
        return discussPostMapper.selectDiscussPosts(userId,offset,limit);
    }

    public int findDicussPostRows(int userId){
        return discussPostMapper.selectDiscussPostRows(userId);
    }
}
