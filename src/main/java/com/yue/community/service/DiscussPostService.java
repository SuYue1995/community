package com.yue.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.yue.community.dao.DiscussPostMapper;
import com.yue.community.entity.DiscussPost;
import com.yue.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    // Caffeine核心接口：Cache，子接口：1. LoadingCache（同步缓存：多线程同步访问该缓存的同一份数据，缓存中没有，让线程排队等，缓存先去DB取，再返回给它们）
    // 2. AsyncLoadingCache（异步缓存，并发取数据）

    // 帖子列表缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;

    // 帖子总数缓存
    private LoadingCache<Integer, Integer> postRowsCache;

    // 首次调用该service时，初始化缓存
    @PostConstruct
    public void init(){
        // 初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {// build方法让参数生效，同时返回LoadingCache对象。CacheLoader接口，当尝试从缓存中取数据时，如果没有Caffeine需要知道如何查取数据，通过load方法
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception { // 缓存数据来源
                        if (key == null || key.length() == 0){
                            throw new IllegalArgumentException("参数错误！");
                        }
                        // 解析key
                        String[] params = key.split(":");
                        if (params == null || params.length != 2){
                            throw new IllegalArgumentException("参数错误！");
                        }
                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        // 此处可以添加二级缓存：Redis -> mysql，先访问Redis，如果没有再访问数据库

                        logger.debug("load post list from DB.");
                        return discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                    }
                });
        // 初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        logger.debug("load post list from DB.");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });

    }

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode){
        if (userId == 0 && orderMode == 1){ // 首页热帖查询
            return postListCache.get(offset + ":" + limit);
        }
        logger.debug("load post list from DB.");
        return discussPostMapper.selectDiscussPosts(userId,offset,limit, orderMode);
    }

    public int findDiscussPostRows(int userId){
        if (userId == 0){ // 首页查询
            return postRowsCache.get(userId); // 无需key，随意写一个userId，恒为0
        }
        logger.debug("load post rows from DB..");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    //保存帖子的业务方法
    public int addDiscussPost(DiscussPost post){
        //参数判空
        if (post == null){
            throw new IllegalArgumentException("参数不能为空!");
        }

        //转义html标签
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle())); //Spring自带的html工具，自动将标签转义为转义字符
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));

        //对post中的title，content数据进行敏感词过滤
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        // 插入数据
        return discussPostMapper.insertDiscussPost(post);
    }

    //查询帖子详细信息
    public DiscussPost findDiscussPostById(int id){
        return discussPostMapper.selectDiscussPostById(id);
    }

    // 更新帖子的回复数量
    public int updateCommentCount(int id, int commentCount){
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    // 修改帖子类型
    public int updateType(int id, int type){
        return discussPostMapper.updateType(id, type);
    }

    // 修改帖子状态
    public int updateStatus(int id, int status){
        return discussPostMapper.updateStatus(id, status);
    }

    // 修改帖子分数
    public int updateScore(int id, double score){
        return discussPostMapper.updateScore(id, score);
    }
}
