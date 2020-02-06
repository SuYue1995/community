package com.yue.community.service;

import com.yue.community.dao.AlphaDao;
import com.yue.community.dao.DiscussPostMapper;
import com.yue.community.dao.UserMapper;
import com.yue.community.entity.DiscussPost;
import com.yue.community.entity.User;
import com.yue.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;

@Service // 希望该Bean被容器管理，业务组件由@Service注解
//@Scope("prototype") // 作用范围，多个实例
public class AlphaService {

    private Logger logger = LoggerFactory.getLogger(AlphaService.class);

    @Autowired
    private AlphaDao alphaDao; //处理查询业务时调用

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

    public AlphaService(){
        System.out.println("实例化AlphaService");
    }

    @PostConstruct //该方法在构造器之后调用。初始化方法通常是在构造器之后调用，用来初始化某些数据
    public void init(){
        System.out.println("初始化AlphaService");
    }

    @PreDestroy // 在销毁对象之前调用，释放某些资源。销毁之后无法调用
    public void destroy(){
        System.out.println("销毁AlphaService");
    }

    public String find(){
        return alphaDao.select();
    }

    // Spring 声明式事务管理示例
    //通过注解的方式管理事务，使其成为一个整体，任何地方报错都要回滚回去。
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED) //通过参数设定隔离级别和传播机制。
    //传播机制：业务方法可能会调用另外组件的业务方法，业务方法A可能调用业务B，而AB两个业务都有可能加上Transactional管理事务，那么B业务的事务以谁的为准？
    //传播机制常用的有三个：REQUIRED：支持当前事务（外部事务），A调B，对B来说，A就是当前事务。如果不存在则创建新事务。
    // REQUIRES_NEW：创建一个新事务，并暂停当前事务（外部事务）。B无视A的事务，创建新事务，按照自己的方式进行。
    // NESTED：如果当前存在事务（外部事物），则嵌套在该事务中执行（独立的提交和回滚），如果不存在，则同REQUIRED一样。A调B，A有事务，就嵌套在A的事务中执行，B在执行时有独立提交和回滚
    public Object save1(){
        // 新增用户
        User user = new User();
        user.setUsername("alpha");
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5("123")+user.getSalt());
        user.setEmail("alpha@qq.com");
        user.setHeaderUrl("http://image.nowcoder.com/head/99t.png");
        user.setCreateTime(new Date());
        userMapper.insertUser(user);
        // 新增帖子
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());//正常情况下，向数据库中插入数据之后，Mybatis会向数据库要这个id，得到id之后会自动添加到该对象中，所以可以直接getId
        post.setTitle("hello");
        post.setContent("新人报道");
        post.setCreateTime(new Date());
        discussPostMapper.insertDiscussPost(post);

        //手动造错，查看事务是否会回滚
        Integer.valueOf("abc");
        return "ok";
    }


    // Spring 编程式事务管理示例
    public Object save2(){
        // 通过transactionTemplate设置隔离级别和传播机制
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        //执行sql保证事务
        return transactionTemplate.execute(new TransactionCallback<Object>() {//回调接口，泛型，指定期望返回的类型
            @Override //实现接口中的方法
            public Object doInTransaction(TransactionStatus transactionStatus) { // 回调方法，transactionTemplate底层自动调用
                // 新增用户
                User user = new User();
                user.setUsername("beta");
                user.setSalt(CommunityUtil.generateUUID().substring(0,5));
                user.setPassword(CommunityUtil.md5("123")+user.getSalt());
                user.setEmail("beta@qq.com");
                user.setHeaderUrl("http://image.nowcoder.com/head/99t.png");
                user.setCreateTime(new Date());
                userMapper.insertUser(user);
                // 新增帖子
                DiscussPost post = new DiscussPost();
                post.setUserId(user.getId());//正常情况下，向数据库中插入数据之后，Mybatis会向数据库要这个id，得到id之后会自动添加到该对象中，所以可以直接getId
                post.setTitle("你好");
                post.setContent("我是新人");
                post.setCreateTime(new Date());
                discussPostMapper.insertDiscussPost(post);

                //手动造错，查看事务是否会回滚
                Integer.valueOf("abc");
                return "ok";
            }
        });
    }

    // @Async让该方法在多线程环境下，被异步调用。启动一个线程运行该方法，和主线程并发执行
    @Async
    public void execute1(){
        logger.debug("execute1");
    }

    // @Scheduled注解定时任务，延迟10m，频率1m。自动调用，无需主动调用
    @Scheduled(initialDelay = 10000, fixedRate = 1000)
    public void execute2(){
        logger.debug("execute2");
    }
}
