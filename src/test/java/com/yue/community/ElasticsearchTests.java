package com.yue.community;

import com.yue.community.dao.DiscussPostMapper;
import com.yue.community.dao.elasticsearch.DiscussPostRepository;
import com.yue.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticsearchTests {

    @Autowired
    private DiscussPostMapper discussPostMapper; // 从数据库中取数据

    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    // 往es服务器中添加数据
    @Test
    public void testInsert(){
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(241));
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(242));
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(243));
    }

    // 插入多条数据
    @Test
    public void testInsertList(){
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(101, 0, 100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(102, 0, 100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(103, 0, 100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(111, 0, 100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(112, 0, 100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(131, 0, 100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(132, 0, 100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(133, 0, 100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(134, 0, 100));
    }

    //修改数据
    @Test
    public void testUpdate(){
        DiscussPost discussPost = discussPostMapper.selectDiscussPostById(231);
        discussPost.setContent("我是新人，使劲灌水");
        discussPostRepository.save(discussPost);
    }

    // 删除数据
    @Test
    public void testDelete(){
//        discussPostRepository.deleteById(231);
        // 删除全部数据
        discussPostRepository.deleteAll();
    }

    // 搜索
    @Test
    public void testSearchByRepository(){
        // 构造搜索条件、结果排序、分页、高亮显示（给匹配到的词添加标签，文本显示到网页上添加样式即可显示高亮）等
        SearchQuery searchQuery = new NativeSearchQueryBuilder() // 用Builder构造SearchQuery实现类
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content")) // QueryBuilders构建搜索条件,multiMatchQuery多个字段同时匹配
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC)) // 结果排序条件：type（置顶）、score（帖子价值，加精）、create_time（创建时间）
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 10)) // 构造分页查询 (起始页， 每页数据条数)
                .withHighlightFields( // 高亮显示字段
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"), // 给匹配的title关键字添加前置、后置标签<em></em>
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build(); // NativeSearchQueryBuilder.build方法执行，返回SearchQuery的实现类

        // Repository底层调用以下ElasticsearchTemplate命令去查询数据，查询到的数据由SearchResultMapper进行处理
        // elasticsearchTemplate.queryForPage(searchQuery, class,SearchResultMapper);
        // 底层获取到高亮显示的值，但是没有返回。


        Page<DiscussPost> page = discussPostRepository.search(searchQuery);  // 分页数据用page对象进行封装. page中存放多个实体类
        System.out.println(page.getTotalElements()); // 查询到的数据量
        System.out.println(page.getTotalPages()); // 按照分页条件，一共查询到数据的页数
        System.out.println(page.getNumber()); // 当前的页数
        System.out.println(page.getSize()); // 每页显示的数据数量
        for (DiscussPost post : page) {
            System.out.println(post);
        }
    }

    // 测试通过Template进行搜索
    @Test
    public void testSearchByTemplate(){
        // 构造搜索条件、结果排序、分页、高亮显示（给匹配到的词添加标签，文本显示到网页上添加样式即可显示高亮）等
        SearchQuery searchQuery = new NativeSearchQueryBuilder() // 用Builder构造SearchQuery实现类
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content")) // QueryBuilders构建搜索条件,multiMatchQuery多个字段同时匹配
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC)) // 结果排序条件：type（置顶）、score（帖子价值，加精）、create_time（创建时间）
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 10)) // 构造分页查询 (起始页， 每页数据条数)
                .withHighlightFields( // 高亮显示字段
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"), // 给匹配的title关键字添加前置、后置标签<em></em>
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build(); // NativeSearchQueryBuilder.build方法执行，返回SearchQuery的实现类

        Page<DiscussPost> page = elasticsearchTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse searchResponse, Class<T> aClass, Pageable pageable) {
                // 通过response获取搜索命中的数据
                SearchHits hits = searchResponse.getHits();
                if (hits.getTotalHits() <= 0){ // 数据量<=0，没有查询到数据
                    return null;
                }

                // 遍历命中的数据，逐一处理，存在list中
                List<DiscussPost> list = new ArrayList<>();
                for (SearchHit hit : hits){
                    // 每得到一个数据，将数据包装到实体类当中返回
                    DiscussPost discussPost = new DiscussPost();
                    // 命中数据格式为JSON，SearchHit对象将JSON数据封装为Map，从hit中调用map得到每一个字段的值
                    String id = hit.getSourceAsMap().get("id").toString();
                    discussPost.setId(Integer.valueOf(id));

                    String userId = hit.getSourceAsMap().get("userId").toString();
                    discussPost.setUserId(Integer.valueOf(userId));

                    // 如果直接封装高亮部分，可能title\content没有匹配到，存在问题。
                    // 先获取原始的title和content，封装到post里。然后再获取到高亮部分，如果有就覆盖，没有就不操作。
                    String title = hit.getSourceAsMap().get("title").toString();
                    discussPost.setTitle(title);

                    String content = hit.getSourceAsMap().get("content").toString();
                    discussPost.setContent(content);

                    String status = hit.getSourceAsMap().get("status").toString();
                    discussPost.setStatus(Integer.valueOf(status));

                    String createTime = hit.getSourceAsMap().get("createTime").toString();
                    discussPost.setCreateTime(new Date(Long.valueOf(createTime)));

                    String commentCount = hit.getSourceAsMap().get("commentCount").toString();
                    discussPost.setCommentCount(Integer.valueOf(commentCount));

                    // 处理高亮显示的结果
                    HighlightField titleField = hit.getHighlightFields().get("title");
                    if (titleField != null){
                        discussPost.setTitle(titleField.getFragments()[0].toString()); // getFragments返回数组，可能一个title匹配到多条，只取第一段即可
                    }

                    HighlightField contentField = hit.getHighlightFields().get("content");
                    if (contentField != null){
                        discussPost.setContent(contentField.getFragments()[0].toString());
                    }
                    list.add(discussPost);
                }

                // 返回数据既包含list，又包含其他数据，是一个聚合的数据。构造AggregatedPage的实现类。参数参考repository源码
                return new AggregatedPageImpl(list, pageable,
                        hits.getTotalHits(), searchResponse.getAggregations(), searchResponse.getScrollId(), hits.getMaxScore());
            }
        });

        System.out.println(page.getTotalElements()); // 查询到的数据量
        System.out.println(page.getTotalPages()); // 按照分页条件，一共查询到数据的页数
        System.out.println(page.getNumber()); // 当前的页数
        System.out.println(page.getSize()); // 每页显示的数据数量
        for (DiscussPost post : page) {
            System.out.println(post);
        }
    }
}
