package com.yue.community.entity;

import java.util.HashMap;
import java.util.Map;

public class Event {

    private String topic; // 主题
    private int userId; // 触发事件的用户
    private int entityType; // 事件发生的实体
    private int entityId;
    private int entityUserId; // 实体作者
    private Map<String, Object> data = new HashMap<>(); // 其他数据存储在map中，扩展性

    // 所有set方法，更改范围类型为Event，返回该对象，方便于定义该实例的属性参数

    public String getTopic() {
        return topic;
    }

    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Event setData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
}
