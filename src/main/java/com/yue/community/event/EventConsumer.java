package com.yue.community.event;

import com.alibaba.fastjson.JSONObject;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.yue.community.entity.DiscussPost;
import com.yue.community.entity.Event;
import com.yue.community.entity.Message;
import com.yue.community.service.DiscussPostService;
import com.yue.community.service.ElasticsearchService;
import com.yue.community.service.MessageService;
import com.yue.community.util.CommunityConstant;
import com.yue.community.util.CommunityUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService; // 将消息数据存入message表中

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    // 生成图片命令
    @Value("${wk.image.command}")
    private String wkImagecommand;

    // 生成图片存放位置
    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${qiniu.key.aceess}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;

    @Value("${qiniu.bucket.share.url}")
    private String shareBucketUrl;

    // 注入定时任务线程池
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

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


    // 消费删帖事件
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record){
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

        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }

    // 消费分享事件
    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShareMessage(ConsumerRecord record){
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

        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        // 拼命令
        String cmd = wkImagecommand + " --quality 75 " + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;

        try {
            Runtime.getRuntime().exec(cmd); // 生成图片耗时，下一句话先执行
            logger.info("生成长图成功：" + cmd); // 主线程处理这句会早于上面生成图片
        } catch (IOException e) {
            logger.error("生成长图失败：" + e.getMessage());
        }

        // 不能直接上传到云服务器，因为Runtime.getRuntime().exec(cmd)生成图片耗时，代码执行到上传部分，可能图片还没有生成成功
        // 启动定时器，每隔一段时间查看图片是否生成，知道生成成功再上传；如果长时间未生成，则认为生成有问题，取消上传
        // 此处分布式部署也可以用Scheduler，因为这部分逻辑不是每个服务器都执行，consumer有抢占机制。五台服务器部署五个consumer，分享事件发出后，只有一个consumer抢到处理，其他服务器不进行处理。
        // 所以只在一个服务器上执行，其他服务器不执行。在这个服务器上启动定时器，不会和其他产生关联。
        // 之前说的是服务一启动，定时器自动运行，5个服务器都运行定时器。这里只有抢到这个消息的服务器会启动定时器。

        // 启动定时器，监视该图片，一旦生成了，则上传至云服务器
        UploadTask task = new UploadTask(fileName, suffix);
        Future future = taskScheduler.scheduleAtFixedRate(task, 500); // 启用定时器，得到返回值Future，封装了任务状态，还可用来停止定时器。
        task.setFuture(future); // 500ms才执行，把future传给task，task在执行时可通过future停止定时器
    }

    // 定义上传图片线程体
    class UploadTask implements Runnable{

        // 文件名
        private String fileName;

        // 文件后缀
        private String suffix;

        // 启动任务的返回值Future
        private Future future;

        // 开始时间
        private long startTime;

        // 上传次数
        private int uploadTimes;

        // 生成有参构造器，强制传入两个必要参数
        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() {
            // 考虑失败的可能性：1. 生成图片失败 2. 上传云服务器失败。通过startTime、uploadTimes判断，如果时间>30s/上传次数=3，则取消任务，上传失败

            // 方法反复调用，处理逻辑和递归相似，先判断终止条件
            // 生成图片失败，导致时间过长
            if (System.currentTimeMillis() - startTime > 30000){
                logger.error("执行时间过长，终止任务：" + fileName);
                future.cancel(true);
                return;
            }
            // 上传失败，上传次数达到3次取消任务
            if (uploadTimes >= 3){
                logger.error("上传次数过多，终止任务：" + fileName);
                future.cancel(true);
                return;
            }

            // 本地存放文件的路径
            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);
            if (file.exists()){ // 图片存在
                logger.info(String.format("开始第%d次上传[%s].", ++uploadTimes, fileName));
                // 上传->凭证->文件名+响应信息
                // 设置响应信息
                StringMap policy = new StringMap();
                policy.put("returnBody", CommunityUtil.getJSONString(0));
                // 生成上传凭证
                Auth auth = Auth.create(accessKey, secretKey);
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);
                // 指定上传机房
                UploadManager manager = new UploadManager(new Configuration(Region.region1())); // region1为华北机房
                try {
                    // 开始上传图片
                    Response response = manager.put(
                            path, fileName, uploadToken, null, "image/" + suffix, false);
                    // 处理响应结果，将字符串转为json
                    JSONObject json = JSONObject.parseObject(response.bodyString());
                    if (json == null || json.get("code") == null || !json.get("code").toString().equals("0")){ // 如果返回数据为空，或没有code，或code不为0。上传失败
                        logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                    }else { // 上传成功
                        logger.info(String.format("第%d次上传成功[%s].", uploadTimes, fileName));
                        future.cancel(true);
                    }
                }catch (QiniuException e){
                    logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName) + " " + e.getMessage());
                }
            }else { // 图片不存在
                logger.info("等待图片生成[" +fileName + "].");
            }
        }
    }

}
