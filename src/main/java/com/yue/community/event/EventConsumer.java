package com.yue.community.event;

import com.alibaba.fastjson.JSONObject;
import com.yue.community.entity.DiscussPost;
import com.yue.community.entity.Event;
import com.yue.community.entity.Message;
import com.yue.community.service.DiscussPostService;
import com.yue.community.service.ElasticsearchService;
import com.yue.community.service.MessageService;
import com.yue.community.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService; // 将消息数据存入message表中

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    // 方法 <--> 主题，多对多关系。可以一个方法处理一个主题，多个主题。一个主题被多个方法处理等等
    // 因为对于点赞、关注、回复三类，系统发送的消息相似，所有在一个方法内处理
    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record){
        if (record == null){
            logger.error("消息内容为空");
            return;
        }
        // 将json字符串恢复为event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null){
            logger.error("消息格式错误");
            return;
        }
        // 发送站内通知，即将信息插入到message表中
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        // content存储JSON字符串，即将来用于拼出在网页上显示完整信息的条件。“用户suyue评论了你的帖子”+帖子链接，所以map存以下信息
        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId()); // 出发事件的用户id，即suyue的id
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());
        // 将event的其他信息存入map
        if (!event.getData().isEmpty()){
            for (Map.Entry<String, Object> entry : event.getData().entrySet()){
                content.put(entry.getKey(), entry.getValue());
            }
        }

        message.setContent(JSONObject.toJSONString(content));

        // 将message存入数据库
        messageService.addMessage(message);
    }

    // 消费发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record){
        if (record == null){
            logger.error("消息内容为空");
            return;
        }
        // 将json字符串恢复为event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null){
            logger.error("消息格式错误");
            return;
        }

        // 从事件的消息里得到帖子id，查询到对应的帖子，存储到es服务器
        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(post);
    }


}
