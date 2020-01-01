package com.yue.community.service;

import com.yue.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    // 点赞
    public void like(int userId, int entityType, int entityId, int entityUserId){
//        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
//        // 判断用户是否点赞
//        boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
//        if (isMember){ // 如果已点赞，则取消点赞
//            redisTemplate.opsForSet().remove(entityLikeKey, userId);
//        }else { // 如果未点赞，则点赞
//            redisTemplate.opsForSet().add(entityLikeKey, userId);
//        }
        // 重构点赞功能，编程式事务管理
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);
                // 判断用户是否点赞，查询放在事务外，因为事务内不会立即得到结果
                boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId);
                // 开启事务
                operations.multi();
                if (isMember){
                    operations.opsForSet().remove(entityLikeKey, userId);
                    operations.opsForValue().decrement(userLikeKey);
                }else {
                    operations.opsForSet().add(entityLikeKey, userId);
                    operations.opsForValue().increment(userLikeKey);
                }
                return operations.exec();
            }
        });
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

    // 查询某个用户获得的赞的数量
    public int findUserLikeCount(int userId){
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        System.out.println(userLikeKey);
        Integer count = (Integer)redisTemplate.opsForValue().get(userLikeKey);
        System.out.println(count == null? 0 : count.intValue());
        return count == null? 0 : count.intValue();
    }
}
