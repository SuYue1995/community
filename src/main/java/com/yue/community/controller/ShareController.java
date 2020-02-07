package com.yue.community.controller;

import com.yue.community.entity.Event;
import com.yue.community.event.EventProducer;
import com.yue.community.util.CommunityConstant;
import com.yue.community.util.CommunityUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ShareController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(ShareController.class);

    // 生成图片时间较长，是异步方式。用事件驱动，controller将分享事件丢给kafka，由kafka异步实现即可。
    @Autowired
    private EventProducer eventProducer;

    // 域名
    @Value("${community.path.domain}")
    private String domain;

    // 项目访问名
    @Value("${server.servlet.context-path}")
    private String contextPath;

    // 生成图片存放位置
    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${qiniu.bucket.share.url}")
    private String shareBucketUrl;

    @RequestMapping(path = "/share", method = RequestMethod.GET)
    @ResponseBody
    public String share(String htmlUrl){ // 实际中，分享的功能需要传入，根据功能生成图片。根据功能找到功能对应的模板的路径
        // 生成随机文件名
        String fileName = CommunityUtil.generateUUID();

        // 异步方式，构建Event，生成长图
        Event event = new Event()
                .setTopic(TOPIC_SHARE)
                .setData("htmlUrl", htmlUrl)
                .setData("fileName", fileName)
                .setData("suffix", ".png");
        eventProducer.fireEvent(event);

        // 返回访问路径
        Map<String, Object> map = new HashMap<>();
//        map.put("shareUrl", domain + contextPath + "/share/image/" + fileName);
        // 返回云服务器的url
        map.put("shareUrl", shareBucketUrl + "/" + fileName);
        return CommunityUtil.getJSONString(0, null, map);
    }

    // 废弃：该方法从本地读取长图传给客户端，现改为从云服务器中读取图片
    // 获取长图
    @RequestMapping(path = "/share/image/{fileName}", method = RequestMethod.GET)
    public void getShareImage(@PathVariable("fileName") String fileName, HttpServletResponse response){
        if (StringUtils.isBlank(fileName)){
            throw new IllegalArgumentException("文件名不能为空！");
        }
        response.setContentType("image/png"); // ("文件/格式")
        File file = new File(wkImageStorage + "/" + fileName + ".png");
        try {
            OutputStream os = response.getOutputStream(); // 获取输出流
            FileInputStream fis = new FileInputStream(file); // 将文件转换为输入流
            byte[] buffer = new byte[1024]; // 缓冲区
            int b = 0; // 游标
            while((b = fis.read(buffer)) != -1){ // 每次去取数据读到buffer里，赋值给游标b，!=-1表示读到数据，输出给输出流
                os.write(buffer, 0 ,b);
            }
        } catch (IOException e) {
            logger.error("获取长图失败：" + e.getMessage());
        }
    }

}
