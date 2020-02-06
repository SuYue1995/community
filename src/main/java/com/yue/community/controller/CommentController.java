package com.yue.community.controller;

import com.yue.community.entity.Comment;
import com.yue.community.entity.DiscussPost;
import com.yue.community.entity.Event;
import com.yue.community.event.EventProducer;
import com.yue.community.service.CommentService;
import com.yue.community.service.DiscussPostService;
import com.yue.community.util.CommunityConstant;
import com.yue.community.util.HostHolder;
import com.yue.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping(path = "/comment")
public class CommentController implements CommunityConstant{

    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private RedisTemplate redisTemplate;

    // 处理新增评论的请求
    @RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment){

        //comment中entityType，entityId，content三个属性值通过页面传入，对其他属性值进行补充
        comment.setUserId(hostHolder.getUser().getId()); // 后面统一处理异常，权限认证，保证只有登录用户才能回帖
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        //  触发评论事件
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId", discussPostId); // 因为系统消息需要链接到帖子页面，所以需要存入帖子id到map中
        if (comment.getEntityType() == ENTITY_TYPE_POST){ // 如果是评论帖子，则传入帖子的userId
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }else if (comment.getEntityType() == ENTITY_TYPE_COMMENT){ // 如果评论的是评论
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        // 调用生产者，处理事件，发送消息
        eventProducer.fireEvent(event); // 直接丢掉队列中，线程逐个处理；继续处理下面的业务

        // 如果评论给帖子，触发发帖事件，将修改的帖子传到es服务器覆盖原来的数据
        if (comment.getEntityType() == ENTITY_TYPE_POST){
            // 创建事件对象
            event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(comment.getUserId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);
            // 触发事件
            eventProducer.fireEvent(event);

            // 计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, discussPostId);
        }


        return "redirect:/discuss/detail/" + discussPostId;
    }
}
