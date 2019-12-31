package com.yue.community.util;

public class RedisKeyUtil {

    private static final String SPLIT = ":";
    private static final String PREFIX_ENTITY_LIKE = "like:entity"; // 点赞的实体：帖子、评论

    // 某个实体的赞
    // like:entity:entityType:entityId -> set(userId) // 集合中存放点赞的用户id
    public static String getEntityLikeKey(int entityType, int entityId){
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }
}
