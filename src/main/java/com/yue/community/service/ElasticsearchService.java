package com.yue.community.service;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ElasticsearchService {

    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    // 向ES服务器提交新产生的帖子
    public void saveDiscussPost(DiscussPost post){
        discussPostRepository.save(post);
    }

    // 删除ES服务器中的帖子
    public void deleteDiscussPost(int id){
        discussPostRepository.deleteById(id);
    }

    // 搜索，返回page类型数据，里面封装多条帖子
    public Page<DiscussPost> searchDiscussPost(String keyword, int current, int limit){ // keyword搜索关键字；current当前页数；limit每页显示数据条数
        // 构造搜索条件、结果排序、分页、高亮显示（给匹配到的词添加标签，文本显示到网页上添加样式即可显示高亮）等
        SearchQuery searchQuery = new NativeSearchQueryBuilder() // 用Builder构造SearchQuery实现类
                .withQuery(QueryBuilders.multiMatchQuery(keyword, "title", "content")) // QueryBuilders构建搜索条件,multiMatchQuery多个字段同时匹配
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC)) // 结果排序条件：type（置顶）、score（帖子价值，加精）、create_time（创建时间）
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(current, limit)) // 构造分页查询 (起始页， 每页数据条数)
                .withHighlightFields( // 高亮显示字段
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"), // 给匹配的title关键字添加前置、后置标签<em></em>
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build(); // NativeSearchQueryBuilder.build方法执行，返回SearchQuery的实现类

        return elasticsearchTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
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
    }
}
