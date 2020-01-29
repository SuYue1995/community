package com.yue.community.dao.elasticsearch;

import com.yue.community.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository // 代码为数据访问层代码，es可视为特殊数据库，所以使用@Repository注解；@Mapper是Mybatis专属注解，@Repository是Spring提供的数据访问层注解
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost, Integer> { // 声明实体类为DiscussPost，实体类中的主键为Integer。
    // 父接口中已定义好对es访问的增删改查各种方法，此处继承父接口，添加注解，然后直接调用即可
}
