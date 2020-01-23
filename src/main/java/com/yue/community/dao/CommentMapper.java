package com.yue.community.dao;


import com.yue.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {

    // 根据实体查询。
    List<Comment> selectCommentByEntity(int entityType, int entityId, int offset, int limit);//分页条件：offset，limit每页显示限制

    //查询数据条目数
    int selectCountByEntity(int entityType, int entityId);

    //新增评论
    int insertComment(Comment comment);

    Comment selectCommentById(int id);

}
