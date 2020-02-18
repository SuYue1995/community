package com.yue.community.controller;

import com.yue.community.entity.DiscussPost;
import com.yue.community.entity.Page;
import com.yue.community.entity.User;
import com.yue.community.service.DiscussPostService;
import com.yue.community.service.LikeService;
import com.yue.community.service.UserService;
import com.yue.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService; //通过discussPostService查到数据中的userId只是Id，通过userService可以查询user的详细信息。

    @Autowired
    private LikeService likeService;

    @RequestMapping(path = "/index", method = RequestMethod.GET) //定义该方法的访问路径，首页index
    //该方法响应的是网页，所以不写@ResponseBody
    public String getIndexPage(Model model, Page page, //String指的是view的名字, 用model携带数据。添加page对象参数
                               @RequestParam(name = "orderMode", defaultValue = "0") int orderMode){ // @RequestParam添加默认值，为原始的排序模式。
        // orderMode参数，通过?传，不是请求体传，因为GET适合用?传。
        //在springmvc框架中，方法的参数都是由DispatcherServlet帮忙初始化的，model，page都是由其实例化，page的数据也是由其注入。
        // 另外它会自动把page注入到mode中，因此在Thymeleaf模板中可以直接访问page中的数据，不需要model再add一次

        //服务器需要设置一些信息
        page.setRows(discussPostService.findDiscussPostRows(0)); //设置总行数
        // 需要在路径上拼orderMode，路径返回到模板，模板用到路径，在此基础上拼分页参数。
        page.setPath("/index?orderMode=" + orderMode);//页面复用路径

        //处理请求的逻辑，查询
        List<DiscussPost> list = discussPostService.findDiscussPosts(0,page.getOffset(),page.getLimit(), orderMode); //查询全部数据，所以userId参数为0 //offset,limit从page里取
        //DiscussPost 只有userId，没有用户名，需要在页面展现
        //遍历集合，通过Id查到user，把数据组装一下放在新的集合中，然后返回给页面
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (list != null){
            for (DiscussPost post: list){ //遍历list，每一次遍历，都要把对应数据放在map中
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                User user = userService.findUserById(post.getUserId());
                map.put("user", user);

                // 返回home帖子赞的数量
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);

                discussPosts.add(map);
            }
        }
        //把需要给页面展示的结果装进Model中
        model.addAttribute("discussPosts", discussPosts); //此处不需要再add page
        model.addAttribute("orderMode", orderMode);
        return "/index"; //返回模板的路径，templates/index.html
    }

    // 报错时重定向到500页面
    @RequestMapping(path = "/error", method = RequestMethod.GET)
    public String getErrorPage(){
        return "/error/500";
    }

    // 拒绝访问时的提示页面
    @RequestMapping(path = "/denied", method = RequestMethod.GET)
    public String getDeniedPage(){
        return "/error/404";
    }
}
