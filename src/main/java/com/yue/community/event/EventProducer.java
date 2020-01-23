package com.yue.community.event;

import com.alibaba.fastjson.JSONObject;
import com.yue.community.entity.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    // 处理事件，即发布消息
    public void fireEvent(Event event){
        // 将事件发布到指定的主题
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event)); // 第二个参数为字符串，包含事件对象中的所有数据，所以将event转换为json字符串，消费者得到字符串后再还原回event

    }
}
