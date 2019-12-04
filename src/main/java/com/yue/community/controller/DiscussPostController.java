package com.yue.community.controller;

import com.yue.community.entity.DiscussPost;
import com.yue.community.entity.User;
import com.yue.community.service.DiscussPostService;
import com.yue.community.service.UserService;
import com.yue.community.util.CommunityUtil;
import com.yue.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;

@Controller
@RequestMapping(path = "/discuss")
public class DiscussPostController {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

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

        // 直接返回JSON字符串。 报错的情况，将来统一进行处理
        return CommunityUtil.getJSONString(0, "发布成功");
    }

    //查询帖子详情
    @RequestMapping(path = "/detail/{discussPostId}", method = RequestMethod.GET)
    //返回discuss-detail.html 模板路径，所以没有@ResponseBody，返回类型为String
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model){  // 获取路径中的变量。将结果发送给模板，通过model携带数据
        // 查询帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        // 将帖子发送给模板
        model.addAttribute("post", post);
        // 查询作者
        User user = userService.findUserById(post.getUserId());
        // 将作者发给模板
        model.addAttribute("user", user);
        return "/site/discuss-detail";
    }
}
