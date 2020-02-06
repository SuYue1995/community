package com.yue.community.service;

import com.yue.community.dao.DiscussPostMapper;
import com.yue.community.entity.DiscussPost;
import com.yue.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class DiscussPostService {

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode){
        return discussPostMapper.selectDiscussPosts(userId,offset,limit, orderMode);
    }

    public int findDicussPostRows(int userId){
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    //保存帖子的业务方法
    public int addDiscussPost(DiscussPost post){
        //参数判空
        if (post == null){
            throw new IllegalArgumentException("参数不能为空!");
        }

        //转义html标签
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle())); //Spring自带的html工具，自动将标签转义为转义字符
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));

        //对post中的title，content数据进行敏感词过滤
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        // 插入数据
        return discussPostMapper.insertDiscussPost(post);
    }

    //查询帖子详细信息
    public DiscussPost findDiscussPostById(int id){
        return discussPostMapper.selectDiscussPostById(id);
    }

    // 更新帖子的回复数量
    public int updateCommentCount(int id, int commentCount){
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    // 修改帖子类型
    public int updateType(int id, int type){
        return discussPostMapper.updateType(id, type);
    }

    // 修改帖子状态
    public int updateStatus(int id, int status){
        return discussPostMapper.updateStatus(id, status);
    }

    // 修改帖子分数
    public int updateScore(int id, double score){
        return discussPostMapper.updateScore(id, score);
    }
}
