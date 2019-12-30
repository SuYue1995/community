package com.yue.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class RedisTests {

    @Autowired
    private RedisTemplate redisTemplate;

    // 测试字符串类型访问方式
    @Test
    public void testString(){
        String redisKey = "test:count";
        // 存数据
        redisTemplate.opsForValue().set(redisKey,1);
        // 取数据
        System.out.println(redisTemplate.opsForValue().get(redisKey));
        System.out.println(redisTemplate.opsForValue().increment(redisKey));
        System.out.println(redisTemplate.opsForValue().decrement(redisKey));
    }

    @Test
    public void testHashes(){
        String redisKey = "test:user";
        redisTemplate.opsForHash().put(redisKey, "id", 1);
        redisTemplate.opsForHash().put(redisKey, "username", "zhangsan");
        System.out.println(redisTemplate.opsForHash().get(redisKey, "id"));
        System.out.println(redisTemplate.opsForHash().get(redisKey, "username"));
    }

    @Test
    public void testLists(){
        String redisKey = "test:ids";
        redisTemplate.opsForList().leftPush(redisKey, 101); // 左进
        redisTemplate.opsForList().leftPush(redisKey, 102);
        redisTemplate.opsForList().leftPush(redisKey, 103);

        System.out.println(redisTemplate.opsForList().size(redisKey)); // 个数
        System.out.println(redisTemplate.opsForList().index(redisKey, 0)); // 索引
        System.out.println(redisTemplate.opsForList().range(redisKey, 0 ,2)); // 范围索引

        System.out.println(redisTemplate.opsForList().leftPop(redisKey)); // 左出
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
    }

    @Test
    public void testSet(){
        String redisKey = "test:teachers";

        redisTemplate.opsForSet().add(redisKey, "aaa", "bbb", "ccc", "ddd", "eee", "fff");
        System.out.println(redisTemplate.opsForSet().size(redisKey)); // 集合中的数据个数
        System.out.println(redisTemplate.opsForSet().pop(redisKey));
        System.out.println(redisTemplate.opsForSet().members(redisKey)); // 统计数据
    }

    @Test
    public void testSortedSet(){
        String redisKey = "test:students";
        redisTemplate.opsForZSet().add(redisKey, "aaa", 80);
        redisTemplate.opsForZSet().add(redisKey, "bbb", 90);
        redisTemplate.opsForZSet().add(redisKey, "ccc", 50);
        redisTemplate.opsForZSet().add(redisKey, "ddd", 70);
        redisTemplate.opsForZSet().add(redisKey, "eee", 60);

        System.out.println(redisTemplate.opsForZSet().zCard(redisKey)); // 统计集合中的元素数量
        System.out.println(redisTemplate.opsForZSet().score(redisKey,"bbb"));
        System.out.println(redisTemplate.opsForZSet().rank(redisKey, "bbb")); //正序排序
        System.out.println(redisTemplate.opsForZSet().reverseRank(redisKey, "bbb")); // 倒序排序
        System.out.println(redisTemplate.opsForZSet().range(redisKey, 0 ,2)); // 正序前三名
        System.out.println(redisTemplate.opsForZSet().reverseRange(redisKey, 0 ,2)); // 倒序前三名

    }

    // 测试全局命令
    @Test
    public void testKeys(){
        redisTemplate.delete("test:user");
        System.out.println(redisTemplate.hasKey("test:user"));
        redisTemplate.expire("test:students", 10, TimeUnit.SECONDS);
    }

    // 利用绑定形式，多次访问同一个key
    @Test
    public void testBoundOperations(){
        String redisKey = "test:count";
        BoundValueOperations operations = redisTemplate.boundValueOps(redisKey);// BoundxxxOperations,xxx为访问的数据类型的单词
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        System.out.println(operations.get());
    }

    // 编程式事务
    @Test
    public void testTransactional(){
        Object obj = redisTemplate.execute(new SessionCallback() { // execute需要传入接口示例，此处匿名实现
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String redisKey = "test:tx";
                // 启用事务
                operations.multi();// 启用事务
                operations.opsForSet().add(redisKey, "zhangsan");
                operations.opsForSet().add(redisKey, "lisi");
                operations.opsForSet().add(redisKey, "wangwu");
                System.out.println(operations.opsForSet().members(redisKey)); // 尝试查询
                // 提交事务
                return operations.exec();
            }
        });
        System.out.println(obj);
    }
}
