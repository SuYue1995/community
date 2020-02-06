package com.yue.community.controller;

import com.yue.community.entity.*;
import com.yue.community.event.EventProducer;
import com.yue.community.service.CommentService;
import com.yue.community.service.DiscussPostService;
import com.yue.community.service.LikeService;
import com.yue.community.service.UserService;
import com.yue.community.util.CommunityConstant;
import com.yue.community.util.CommunityUtil;
import com.yue.community.util.HostHolder;
import com.yue.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping(path = "/discuss")
public class DiscussPostController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    //增加帖子请求，异步请求
    @RequestMapping(path = "/add", method = RequestMethod.POST)
    @ResponseBody //返回的是字符串，不是网页
    public String addDicussPost(String title, String content){ //页面传入的只有标题和内容
        //判断是否登录
        User user = hostHolder.getUser();
        if (user == null){
            return CommunityUtil.getJSONString(403, "未登录"); // 403代表没有权限
        }

        //构造实体，然后service保存
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);

        // 触发发帖事件，将新发布的帖子存到es服务器
        // 创建事件对象
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(post.getId());
        // 触发事件
        eventProducer.fireEvent(event);

        // 计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, post.getId());


        // 直接返回JSON字符串。 报错的情况，将来统一进行处理
        return CommunityUtil.getJSONString(0, "发布成功");
    }

    //查询帖子详情
    @RequestMapping(path = "/detail/{discussPostId}", method = RequestMethod.GET)
    //返回discuss-detail.html 模板路径，所以没有@ResponseBody，返回类型为String
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page){  // 获取路径中的变量。将结果发送给模板，通过model携带数据。Page，接受整理分页条件。只要是Java bean，声明在条件当中作为参数，SpringMVC会把这个bean传入model中，在页面中通过model获得
        // 查询帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        // 将帖子发送给模板
        model.addAttribute("post", post);
        // 查询作者
        User user = userService.findUserById(post.getUserId());
        // 将作者发给模板
        model.addAttribute("user", user);
        // 点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount", likeCount);
        // 点赞状态，当前用户是否点赞
        int likeStatus = hostHolder.getUser() == null ? 0 :
                likeService.findEntityLikeStatus(hostHolder.getUser().getId(),ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus", likeStatus);

        // 评论的分页信息
        page.setLimit(5); // 每页显示5条
        page.setPath("/discuss/detail/" + discussPostId);
        page.setRows(post.getCommentCount());//评论数据的条目数，用来计算总页数

        // 评论：给帖子的评论
        // 回复: 给评论的评论

        // 1. 评论列表
        // 分页查询得到集合
        List<Comment> commentList = commentService.findCommentByEntity(
                ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());

        // 2. 评论Vo列表，显示对象的列表
        // 通过userId查询到user对象，用于显示头像，用户名
        // 遍历集合，构造map，把Comment和查询到的依赖数据存进map。对展现的数据进行封装
        List<Map<String, Object>> commentVoList = new ArrayList<>();// Vo: View Object 显示的对象
        // 如果集合非空，进行遍历
        if (commentList != null){
            for (Comment comment: commentList){
                // 一个评论的Vo
                Map<String, Object> commentVo = new HashMap<>(); //用map封装呈现给页面的数据
                // Vo中存评论
                commentVo.put("comment", comment);
                // Vo中存作者
                commentVo.put("user", userService.findUserById(comment.getUserId()));

                // 点赞数量
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount", likeCount);
                // 点赞状态，当前用户是否点赞
                likeStatus = hostHolder.getUser() == null ? 0 :
                        likeService.findEntityLikeStatus(hostHolder.getUser().getId(),ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus", likeStatus);


                // 3. 回复列表
                List<Comment> replyList = commentService.findCommentByEntity(ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE); // 回复列表不分页
                // 4. 回复的Vo列表
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                // 遍历replyList集合去构造replyVoList
                if (replyList != null){
                    for (Comment reply: replyList){
                        Map<String, Object> replyVo = new HashMap<>();
                        // Vo中存回复reply
                        replyVo.put("reply",reply);
                        // Vo中存作者
                        replyVo.put("user", userService.findUserById(reply.getUserId()));
                        // Vo中存回复目标
                        User target = reply.getTargetId() == 0? null:userService.findUserById(reply.getTargetId());
                        replyVo.put("target", target);
                        // 点赞数量
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount", likeCount);
                        // 点赞状态，当前用户是否点赞
                        likeStatus = hostHolder.getUser() == null ? 0 :
                                likeService.findEntityLikeStatus(hostHolder.getUser().getId(),ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeStatus", likeStatus);

                        // 将replyVo装到List里
                        replyVoList.add(replyVo);
                    }
                }
                // 将replyVoList装入到commentVo中，commentVo返回给页面，页面通过commentVo得到需要展示的一切数据
                commentVo.put("replys", replyVoList);

                // 将回复数量存到commentVo中
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount", replyCount);

                // 将commentVo添加到commentVoList中
                commentVoList.add(commentVo);
            }
        }
        model.addAttribute("comments", commentVoList);
        return "/site/discuss-detail";
    }

    // 置顶，异步请求
    @RequestMapping(path = "/top", method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id){
        discussPostService.updateType(id, 1); // 0-普通，1-置顶
        // 将帖子新数据同步到Elasticsearch中
        // 触发发帖事件，将新发布的帖子存到es服务器
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }

    // 加精，异步请求
    @RequestMapping(path = "/wonderful", method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id){
        discussPostService.updateStatus(id, 1); // 0-正常，1-加精，2-拉黑删除
        // 将帖子新数据同步到Elasticsearch中
        // 触发发帖事件，将新发布的帖子存到es服务器
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        // 计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, id);

        return CommunityUtil.getJSONString(0);
    }

    // 删除，异步请求
    @RequestMapping(path = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id){
        discussPostService.updateStatus(id, 2); // 0-正常，1-加精，2-拉黑删除
        // 触发删帖事件，将帖子从ES服务器中删除
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }
}
