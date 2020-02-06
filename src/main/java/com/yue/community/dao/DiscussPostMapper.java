package com.yue.community.dao;

import com.yue.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    // orderMode 排序模式：默认0,1：按热度排
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit, int orderMode); //首页显示所有的帖子，无需userId。将来发开个人主页功能，“我发布的帖子”调用该方法，传入userId。
    //当userId为0时，就不管它，不把这个条件拼到sql语句中；当userId为正常值时，将其拼到sql中。因此实现该方法sql为动态sql，时加时不加。
    //分页功能：sql,limit关键字后面有两个参数，一个是这一页起始行的行号offset，另一个是这一页最多显示多少条数据limit。

    //页数=表中总数据条数/每页数据数，所以需要一个方法可查询出表中总数据数
    int selectDiscussPostRows(@Param("userId") int userId); // userId同上。@Param()该注解是为了给参数取别名，有的参数较长，sql中麻烦可取别名简化
    // 如果该方法只有一个参数，并且在sql动态条件<if>里使用，则必须加Param注解，否则会报错。

    //插入帖子，返回行数
    int insertDiscussPost(DiscussPost discussPost);

    // 根据id查询帖子详细信息
    DiscussPost selectDiscussPostById(int id);

    //更新评论数量
    int updateCommentCount(int id, int commentCount);

    // 修改帖子类型
    int updateType(int id, int type);

    // 修改帖子状态
    int updateStatus(int id, int status);

    // 修改帖子分数
    int updateScore(int id, double score);
}
