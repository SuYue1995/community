package com.yue.community.service;

import com.yue.community.dao.CommentMapper;
import com.yue.community.entity.Comment;
import com.yue.community.util.CommunityConstant;
import com.yue.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class CommentService implements CommunityConstant {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private DiscussPostService discussPostService;

    // 查询某一页评论
    public List<Comment> findCommentByEntity(int entityType, int entityId, int offset, int limit){
        return commentMapper.selectCommentByEntity(entityType, entityId, offset, limit);
    }

    // 查询评论数量
    public int findCommentCount(int entityType, int entityId){
        return commentMapper.selectCountByEntity(entityType, entityId);
    }

    //增加评论
    // 这个方法包括两次dml操作，1.新增评论+2.更新评论数量，进行事务管理，保证两次操作在一个事务范围内，同时成功或失败
    //因为整个方法在一个事务内，为了方便，使用声明式事务管理方法
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment){

        //判空
        if (comment == null){
            throw new IllegalArgumentException("参数不能为空！");
        }

        // 1. 新增评论
        // 对评论内容进行过滤
        //过滤标签
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        // 过滤敏感词
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        // 插入到数据库
        int rows = commentMapper.insertComment(comment);

        // 2.更新帖子的评论数量
        // 评论针对的目标可以是帖子，评论或其他内容。只有针对帖子时，才更新评论数量
        if (comment.getEntityType() == ENTITY_TYPE_POST){
            // 根据实体查询到评论数量
            int count = commentMapper.selectCountByEntity(comment.getEntityType(), comment.getEntityId());
            // 更新帖子数量
            discussPostService.updateCommentCount(comment.getEntityId(), count);
        }

        return rows;
    }

    public Comment findCommentById(int commentId){
        return commentMapper.selectCommentById(commentId);
    }
}
