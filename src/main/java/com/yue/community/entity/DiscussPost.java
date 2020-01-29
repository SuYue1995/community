package com.yue.community.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

// Spring整合es，在底层访问es服务器时，自动将实体数据和es服务器中的索引进行映射
// 映射时，指定indexName索引名，type类型, shards分片, replicas副本。如果检测到没有索引，会自动创建索引，会根据分片、副本配置进行创建，然后再插入数据
@Document(indexName = "discusspost", type = "_doc", shards = 6, replicas = 3) // type在新版本被弱化，此处写死为_doc
public class DiscussPost {

    @Id
    private  int id;

    @Field(type = FieldType.Integer)
    private  int userId;

    // title、content 主要用于搜索，特殊配置
    // e.g. title：互联网校招，存储之后，为这句话建立索引，提炼关键词，用关键词关联这句话，将来通过关键词搜索，通过关联可搜到这句话。
    // 所以存储时，尽量拆分出更多的词条，增大搜索的范围，使用范围较大的分词器ik_max_word，结果可能为：互联、联网、网校、校招
    // 搜索时，不需要拆分太细，尽量猜测意图进行拆分，使用ik_smart分词器
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart") //analyzer、searchAnalyzer为存储、搜索解析器/分词器
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    @Field(type = FieldType.Integer)
    private int type;

    @Field(type = FieldType.Integer)
    private int status;

    @Field(type = FieldType.Date)
    private Date createTime;

    @Field(type = FieldType.Integer)
    private int commentCount;

    @Field(type = FieldType.Double)
    private double score;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "DiscussPost{" +
                "id=" + id +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", createTime=" + createTime +
                ", commentCount=" + commentCount +
                ", score=" + score +
                '}';
    }
}
