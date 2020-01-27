package com.yue.community.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yue.community.entity.Message;
import com.yue.community.entity.Page;
import com.yue.community.entity.User;
import com.yue.community.service.MessageService;
import com.yue.community.service.UserService;
import com.yue.community.util.CommunityConstant;
import com.yue.community.util.CommunityUtil;
import com.yue.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import javax.imageio.ImageIO;
import java.util.*;

@Controller
public class MessageController implements CommunityConstant {

    @Autowired
    private MessageService messageService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    // 私信列表
    @RequestMapping(path = "/letter/list", method = RequestMethod.GET)
    public String getLetterList(Model model, Page page){
        //获取当前用户
        User user = hostHolder.getUser();

        // 查询会话列表
        // 设置分页信息
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCount(user.getId()));

        // 查询会话列表
        List<Message> conversationList = messageService.findConversations(user.getId(), page.getOffset(), page.getLimit());
        // 页面上不管显示消息，还要显示其他额外内容，用map进行封装
        List<Map<String, Object>> conversations = new ArrayList<>();
        // 遍历会话列表
        if (conversationList!=null){
            for (Message message:conversationList){
                //每次遍历，新建map，重构数据
                Map<String, Object> map = new HashMap<>();
                map.put("conversation", message);
                // 消息数量
                map.put("letterCount",messageService.findLetterCount(message.getConversationId()));
                // 未读消息数量
                map.put("unreadCount", messageService.findLetterUnreadCount(user.getId(), message.getConversationId()));
                // 获取与当前用户进行会话的用户id
                int targetId = user.getId() == message.getFromId()? message.getToId(): message.getFromId();
                map.put("target",userService.findUserById(targetId));

                conversations.add(map);
            }
        }
        model.addAttribute("conversations", conversations);

        // 查询当前用户所有未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);// 所有主题的未读数量，topic为null
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);
        return "/site/letter";
    }

    // 私信详情
    @RequestMapping(path = "/letter/detail/{conversationId}", method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Page page, Model model){

        // 设置分页信息
        page.setLimit(5);
        page.setPath("/letter/detail/" + conversationId);
        page.setRows(messageService.findLetterCount(conversationId));

        // 私信列表
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letters = new ArrayList<>();
        if (letterList != null){
            for (Message message:letterList){
                Map<String, Object> map = new HashMap<>();
                map.put("letter", message);
                // 每条消息都显示发消息用户的用户名和头像
                map.put("fromUser",userService.findUserById(message.getFromId()));
                letters.add(map);
            }
        }
        model.addAttribute("letters", letters);
        // 当前用户与之对话的用户，target
        model.addAttribute("target", getLetterTarget(conversationId));

        // 设置已读
        // 将私信列表中的未读消息提取出来，获取其id
        List<Integer> ids = getLetterIds(letterList);
        if (!ids.isEmpty()){
            messageService.readMessage(ids);
        }
        return "/site/letter-detail";

    }

    // 通过conversationId查询与当前用户对话的目标用户
    private User getLetterTarget(String conversationId){
        String[] ids = conversationId.split("_");
        int id0 = Integer.parseInt(ids[0]);
        int id1 = Integer.parseInt(ids[1]);
        if (hostHolder.getUser().getId() == id0){
            return userService.findUserById(id1);
        }else {
            return userService.findUserById(id0);
        }
    }

    // 将私信列表中的未读消息提取出来
    private List<Integer> getLetterIds(List<Message> letterList){
        List<Integer> ids = new ArrayList<>();
        if (letterList!=null){
            for (Message message: letterList){
                // 判断当前用户是不是接受者 and 私信状态是否为未读
                if (hostHolder.getUser().getId() == message.getToId() && message.getStatus() == 0){
                    ids.add(message.getId());
                }
            }
        }
        return ids;
    }

    // 发送私信
    @RequestMapping(value = "/letter/send", method = RequestMethod.POST)
    @ResponseBody // 该方法是异步的，需要加上ResponseBody注解
    public String sendLetter(String toName, String content){ // 收件人的用户名
        // 通过用户名查找id
        User target = userService.findUserByName(toName);
        // 如果目标用户不存在则返回提示信息
        if (target == null){
            return CommunityUtil.getJSONString(1, "目标用户不存在");
        }

        Message message = new Message();
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        // 拼接conversationId，小的在前，大的在后，中间“_”
        if (message.getFromId() < message.getToId()){
            message.setConversationId(message.getFromId()+"_"+message.getToId());
        }else {
            message.setConversationId(message.getToId()+"_"+message.getFromId());
        }
        message.setContent(content);
        message.setCreateTime(new Date());
        // message.status 默认是0，不用设置
        messageService.addMessage(message);

        //如果没有报错，给页面返回一个状态 0。如果报错，将来统一处理异常
        return CommunityUtil.getJSONString(0);
    }

    @RequestMapping(path = "/notice/list", method = RequestMethod.GET)
    public String getNoticeList(Model model){
        User user = hostHolder.getUser();

        // 查询评论类通知
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);
        if (message != null){
            Map<String, Object> messageVO = new HashMap<>();
            messageVO.put("message", message);

            // 将content的JSON字符串还原回对象放入map中
            String content = HtmlUtils.htmlUnescape(message.getContent()); // htmlUnescape 消除转义字符
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class); // 还原为hashmap

            // 将data数据转存入messageVO中，messageVO是最终被模板引用显示数据的
            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));
            messageVO.put("postId", data.get("postId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_COMMENT);
            messageVO.put("count", count);

            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
            messageVO.put("unreadCount", unreadCount);

            model.addAttribute("commentNotice", messageVO);
        }


        // 查询点赞类通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);

        if (message != null){
            Map<String, Object> messageVO = new HashMap<>();
            messageVO.put("message", message);

            // 将content的JSON字符串还原回对象放入map中
            String content = HtmlUtils.htmlUnescape(message.getContent()); // htmlUnescape 消除转义字符
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class); // 还原为hashmap

            // 将data数据转存入messageVO中，messageVO是最终被模板引用显示数据的
            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));
            messageVO.put("postId", data.get("postId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_LIKE);
            messageVO.put("count", count);

            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
            messageVO.put("unreadCount", unreadCount);

            model.addAttribute("likeNotice", messageVO);
        }


        // 查询关注类通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);

        if (message != null){
            Map<String, Object> messageVO = new HashMap<>();
            messageVO.put("message", message);

            // 将content的JSON字符串还原回对象放入map中
            String content = HtmlUtils.htmlUnescape(message.getContent()); // htmlUnescape 消除转义字符
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class); // 还原为hashmap

            // 将data数据转存入messageVO中，messageVO是最终被模板引用显示数据的
            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("count", count);

            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("unreadCount", unreadCount);

            model.addAttribute("followNotice", messageVO);
        }


        // 查询未读消息数量：私信+消息
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(),null); // 所有会话的未读数量，conversationId为空
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);// 所有主题的未读数量，topic为null
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/notice";
    }

    // 通知详情列表
    @RequestMapping(path = "/notice/detail/{topic}", method = RequestMethod.GET)
    public String getNoticeDetatil(@PathVariable("topic") String topic, Page page, Model model){
        // 获取当前用户
        User user = hostHolder.getUser();

        page.setLimit(5); // 每页显示5条数据
        page.setPath("/notice/detail/" + topic);
        page.setRows(messageService.findNoticeCount(user.getId(), topic));

        List<Message> noticeList = messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        // 查询结果中还需要聚集其他数据，处理user实体
        List<Map<String, Object>> noticeVoList = new ArrayList<>();
        if (noticeList != null){
            for (Message notice : noticeList){
                Map<String, Object> map = new HashMap<>();
                //存通知
                map.put("notice", notice);
                // 内容
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
                map.put("user", userService.findUserById((Integer) data.get("userId")));
                map.put("entityType", data.get("entityType"));
                map.put("entityId", data.get("entityId"));
                // 评论、点赞包含postId，关注不包含，统一处理，无postId为null即可
                map.put("postId", data.get("postId"));
                // 通知的作者
                map.put("fromUser", userService.findUserById(notice.getFromId()));

                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices", noticeVoList);

        // 设置已读
        // 获取需要设置已读的id
        List<Integer> ids = getLetterIds(noticeList);
        if (!ids.isEmpty()){
            messageService.readMessage(ids);
        }
        return "/site/notice-detail";
    }

}
