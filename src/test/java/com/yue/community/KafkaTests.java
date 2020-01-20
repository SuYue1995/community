package com.yue.community;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)

public class KafkaTests {

    @Autowired
    private KafkaProducer kafkaProducer;

    @Test
    public void testKafka(){
        // 测试 生产者生产数据，消费者是否能接收到
        // 发送完消息后，不希望程序立刻结束，就看不到消费者消费的过程，所以阻塞当前的主线程。消费者接收到消息，输出打印消息
        // 生产者发消息是主动发送的，何时调用何时发送。消费者处理消息是被动的，一旦队列中有消息就自动处理，可能存在延迟
        kafkaProducer.sendMessage("test", "你好");
        kafkaProducer.sendMessage("test", "Hello World!");

        try {
            Thread.sleep(1000 * 10); // 1000ms * 10 = 10s
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}

// 封装 生产者
@Component // Spring 容器管理这个bean
class KafkaProducer {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    public void sendMessage(String topic, String content){
        kafkaTemplate.send(topic, content);
    }
}

// 封装 消费者
@Component // Spring 容器管理这个bean
class KafkaConsumer {

    @KafkaListener(topics = {"test"}) // Spring 一直监听test主题的数据
    public void handleMessage(ConsumerRecord record){ // 消息封装到ConsumerRecord
        System.out.println(record.value());
    }
}