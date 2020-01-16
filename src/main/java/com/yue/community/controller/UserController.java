package com.yue.community.controller;

import com.yue.community.annotation.LoginRequired;
import com.yue.community.entity.User;
import com.yue.community.service.FollowService;
import com.yue.community.service.LikeService;
import com.yue.community.service.UserService;
import com.yue.community.util.CommunityConstant;
import com.yue.community.util.CommunityUtil;
import com.yue.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@Controller
@RequestMapping("/user") //给这个类声明访问路径
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    //注入上传路径
    @Value("${community.path.upload}")
    private String uploadPath; //声明变量，接受注入的值

    //注入域名
    @Value("${community.path.domain}")
    private String domain;

    //注入项目访问路径（项目名）
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder; //获取当前用户

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    //浏览器通过以下方法访问到个人设置页面
    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET) //声明访问路径，访问普通页面，不提交数据用GET
    public String getSettingPage(){
        return "/site/setting";
    }

    //上传文件
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST) //上传文件，表单的请求方法必须为POST
    public String uploadHeader(MultipartFile headerImg, Model model){ //页面传入一个文件，声明一个MultipartFile；多个文件，声明数组。Model给模板携带数据
        //判断参数传入是否为空
        if (headerImg == null){
            model.addAttribute("error","您还没有选择图片");
            return "/site/setting";
        }

        //上传文件
        //为了避免文件被覆盖，上传的文件名使用不重复的随机名字。名字随机，后缀不变
        //先文件名中获取文件后缀，暂存
        String fileName = headerImg.getOriginalFilename();//用户上传的原始文件名
        //从文件中截取后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));//从最后一个点往后截取
        //判断后缀是否为空
        if (StringUtils.isBlank(suffix)){
            model.addAttribute("error","文件格式不正确！");
            return "/site/setting";
        }
        //生成随机文件名：随机字符串+后缀名
        fileName = CommunityUtil.generateUUID()+suffix;

        //确定文件存放路径
        File dest = new File(uploadPath+"/"+fileName);//根据目标存放位置，初始化File，用于存放数据

        //存储文件，将headerImg写入到File文件里
        try {
            headerImg.transferTo(dest);//将当前文件的内容写入到目标文件中
        } catch (IOException e) {
            //遇到异常，记录日志
            logger.error("上传文件失败" + e.getMessage());
            //抛出异常，将来controller对所以抛出的异常进行统一处理
            throw new RuntimeException("上传文件失败，服务器发生异常！" + e);
        }

        //存储成功，更新当前用户的头像路径 (web访问路径，不是本地路径)
        //http://127.0.0.1:8000(域名)/community/user/header/xxx.png 规定路径格式
        //给用户提供读取图片方法时，按照以上路径处理请求
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName; //拼成允许外界访问的web路径
        userService.updateHeader(user.getId(), headerUrl);

        //更新成功，重定向到首页。
        return "redirect:/index";
    }

    //获取头像
    @RequestMapping(path = "/header/{fileName}",method = RequestMethod.GET)
    //向浏览器响应的是图片，二进制数据。通过流和response主动向浏览器输出，所以返回值为void
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response){ //@PathVariable从路径中解析参数

        // 服务器存放路径
        fileName = uploadPath + "/" + fileName; //fileName变为带上本地路径的全限定名

        // 向浏览器输出图片，需要提前声明输出文件的格式，即文件后缀
        // 解析文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf(".")+1); //从最后一个“.”,往后截取
        //响应图片
        response.setContentType("/image/" + suffix);
        // 响应图片为二进制数据，用字节流
        try (
                //获取输出流
                OutputStream os = response.getOutputStream(); //输出流SpringMVC会自动关系，因为管理response
                //创建文件输入流，读取文件得到输入流
                FileInputStream fis = new FileInputStream(fileName); //java7语法，有close方法，自动加上finally，并在finally中自动关闭
             )
        {
            //不是一个字节一个字节输出，建立缓冲区，每次1024字节，一批一批输出
            byte[] buffer = new byte[1024];
            int b = 0; //游标
            while((b=fis.read(buffer)) != -1) {//利用while循环输出。每次读取最多buffer数据，数据赋值给b，!=-1 表示读取到数据继续，==-1没读到数据结束
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败！" + e.getMessage());
        }
    }

    //修改密码
    @RequestMapping(path = "/updatePassword", method = RequestMethod.POST)
    public String updatePassword(String oldPassword, String newPassword, Model model){

        //获取当前用户
        User user = hostHolder.getUser();

        Map<String, Object> map = userService.updatePassword(user.getId(),oldPassword,newPassword);

        if (map == null || map.isEmpty()){ //修改成功，跳转到登录页面，注销当前登录
            model.addAttribute("msg","密码修改成功，请重新登录");
            model.addAttribute("target","/logout");
            return "/site/operate-result";
        }else { //修改失败，显示错误信息
            model.addAttribute("oldPasswordMsg", map.get("oldPasswordMsg"));
            model.addAttribute("newPasswordMsg",map.get("newPasswordMsg"));
            return "/site/setting";
        }
    }

    // 个人主页
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model){
        User user = userService.findUserById(userId);
        if (user == null){
            throw new RuntimeException("该用户不存在");
        }

        // 将用户传给页面
        model.addAttribute("user", user);
        // 点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        // 关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        // 当前登录用户是否已关注该用户
        boolean hasFollowed = false;
        if (hostHolder.getUser()!=null){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);


        return "/site/profile";
    }


}
