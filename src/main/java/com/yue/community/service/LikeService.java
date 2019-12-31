package com.yue.community.service;

import com.yue.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    // 点赞
    public void like(int userId, int entityType, int entityId){
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        // 判断用户是否点赞
        boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
        if (isMember){ // 如果已点赞，则取消点赞
            redisTemplate.opsForSet().remove(entityLikeKey, userId);
        }else { // 如果未点赞，则点赞
            redisTemplate.opsForSet().add(entityLikeKey, userId);
        }
    }

     // 查询某实体点赞的数量
    public long findEntityLikeCount(int entityType, int entityId){
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    // 查询某用户对某实体的点赞状态
    public int findEntityLikeStatus(int userId, int entityType, int entityId){// 返回int，更具备扩展性，将来可以点踩
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1:0; // 1 点赞，0 未点赞
    }
}
