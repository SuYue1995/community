package com.yue.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.*;
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

    // HyperLogLog 统计20万个重复数据的独立总数
    @Test
    public void testHyperLogLog(){
        // 先存储数据
        String redisKey = "test:hll:01";

        // 造20w数据
        for (int i = 1; i <= 100000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey, i);
        }
        // 造重复数据
        for (int i = 1; i <= 100000; i++) {
            int r = (int) (Math.random() * 100000 + 1); //[1-100000]随机数
            redisTemplate.opsForHyperLogLog().add(redisKey, r);
        }

        // 统计独立数据量
        long size = redisTemplate.opsForHyperLogLog().size(redisKey);
        System.out.println(size);
    }

    // 将三组数据合并，再统计合并后的重复数据的独立总数
    @Test
    public void testHyperLogLogUnion(){
        String redisKey2= "test:hll:02";
        for (int i = 1; i <= 10000 ; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey2, i);
        }

        String redisKey3= "test:hll:03";
        for (int i = 5001; i <= 15000 ; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey3, i);
        }

        String redisKey4= "test:hll:04";
        for (int i = 10001; i <= 20000 ; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey4, i);
        }

        // HyperLogLog对数据合并，是将多组数据合并以后，再存回到Redis里，类型也是HyperLogLog
        String unionKey = "test:hll:union";
        redisTemplate.opsForHyperLogLog().union(unionKey, redisKey2, redisKey3, redisKey4);

        long size = redisTemplate.opsForHyperLogLog().size(unionKey);
        System.out.println(size);
    }

    // 统计一组数据的布尔值
    @Test
    public void testBitmap(){
        String redisKey = "test:bm:01";

        // 记录数据
        // bitmap不是特殊的数据结构，是对String的特殊操作，按位存。默认存0，所以只设定true 1即可
        redisTemplate.opsForValue().setBit(redisKey, 1, true);
        redisTemplate.opsForValue().setBit(redisKey, 4, true);
        redisTemplate.opsForValue().setBit(redisKey, 7, true);

        // 查询某一位的结果
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 2));

        // 统计true总数
        // opsForValue无法访问，需要redis底层连接才能访问统计总数的方法
        //获取redis底层连接
        Object obj = redisTemplate.execute(new RedisCallback() {// execute执行一个redis命令，需要传入回调的接口，做一个匿名实现
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException { // redis连接传进来
                return redisConnection.bitCount(redisKey.getBytes()); // 返回给execute方法。按位统计，需要传入byte数组，统计数组中1的个数
            }
        });

        System.out.println(obj);
    }

    // 统计三组数据的布尔值，并对这3组数据做OR运算
    @Test
    public void testBitmapOperation(){
        String redisKey2 = "test:bm:02";
        redisTemplate.opsForValue().setBit(redisKey2, 0, true);
        redisTemplate.opsForValue().setBit(redisKey2, 1, true);
        redisTemplate.opsForValue().setBit(redisKey2, 2, true);

        String redisKey3 = "test:bm:03";
        redisTemplate.opsForValue().setBit(redisKey3, 2, true);
        redisTemplate.opsForValue().setBit(redisKey3, 3, true);
        redisTemplate.opsForValue().setBit(redisKey3, 4, true);

        String redisKey4 = "test:bm:04";
        redisTemplate.opsForValue().setBit(redisKey4, 4, true);
        redisTemplate.opsForValue().setBit(redisKey4, 5, true);
        redisTemplate.opsForValue().setBit(redisKey4, 6, true);

        // 运算后产生新的结果，存在新的地方，新的key去接收
        String redisKey = "test:bm:or";
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                redisConnection.bitOp(RedisStringCommands.BitOperation.OR, // 指定运算符
                        redisKey.getBytes(), // 指定结果存在哪一个key中
                        redisKey2.getBytes(), redisKey3.getBytes(), redisKey4.getBytes());
                return redisConnection.bitCount(redisKey.getBytes());
            }
        });
        System.out.println(obj);
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 2));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 3));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 4));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 5));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 6));
    }
}
