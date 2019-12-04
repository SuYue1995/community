package com.yue.community.util;


import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map; 

@Component //托管到容器，使其在各个层次都可用，非某层专用
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    //敏感词替换符号
    private static final String REPLACEMENT = "***";

    //初始化前缀树
    //初始化根节点
    private TrieNode rootNode = new TrieNode();

    @PostConstruct //表示该方法为初始化方法，当容器实例化SensitiveFilter这个bean，调用构造器之后，这个方法会自动调用
    //在服务启动时候，SensitiveFilter这个方法就被初始化，init这个方法就被调用，树形结构就构造好，后续直接使用筛选敏感词
    public void init(){
        //读取文件中的敏感词，加载到字节流，字节流在finally中关闭
        try(
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");//获取类加载器，类加载器从类路径下加载资源，也就是target/classes目录之下。程序一编译，所有代码都会编译到classes目录之下，包括配置文件
                //字节流转换成字节流，然后转换成缓冲流
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ){
            //读取敏感词
            String keyword;
            while ( (keyword = reader.readLine()) != null){
                // 添加到前缀树
                this.addKeyword(keyword);
            }
        }catch (IOException e){
            logger.error("加载敏感词文件失败：" + e.getMessage());
        }
    }

    //将敏感词添加到前缀树中，根据敏感词初始化前缀树
    private void addKeyword(String keyword){
        //创建临时节点，相当于指针，默认指向root，不断指向下一个字符，构造树的下一级
        TrieNode tempNode = rootNode;
        // 遍历单词中的字符
        for (int i = 0; i < keyword.length(); i++){
            char c = keyword.charAt(i);
            // 判断该字符是否为当前节点的子节点，如果不是初始化子节点，如果是直接使用
            TrieNode subNode = tempNode.getSubNode(c);
            if (subNode == null){
                //初始化子节点
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }
            // 指针指向子节点，进入下一轮循环
            tempNode = subNode;

            //设置结束标识
            if (i == keyword.length()-1){
                tempNode.setKeywordEnd(true);
            }
        }
    }


    /**
     * 外界调用的过滤敏感词的方法，返回覆盖掉敏感词的字符串
     * @param text 待过滤的文本
     * @return 过滤后的为本
     */
    public String filter(String text){
        // 判空
        if (StringUtils.isBlank(text)){
            return null;
        }

        // 过滤过程，依赖三个指针，声明三个变量
        //指针1，指向树，默认指向根节点
        TrieNode tempNode = rootNode;
        // 指针2， 指向字符串，默认指向字符串首位
        int begin = 0;
        // 指针3，指向字符串，默认首位
        int position = 0;
        // 记录结果，变长字符串StringBuilder，效率比String高
        StringBuilder sb = new StringBuilder();

        // 遍历字符串，当指针2到达最后字符时，结束遍历
        while(begin < text.length()){
            char c = text.charAt(position);

            //跳过符号
            if (isSymbol(c)){
                //若指针1处于根节点，将此符号记入结果，让指针2向下走一步
                if (tempNode == rootNode){
                    sb.append(c);
                    begin ++;
                }
                // 无论符号在开头或中间，指针3都向下走一步
                position ++;
                continue;
            }

            //字符不是符号
            //检查下级节点
            tempNode = tempNode.getSubNode(c);
            if (tempNode == null){
                // 以begin开头的字符串不是敏感词
                // 记录begin开头的字符串
                sb.append(text.charAt(begin));
                // 进入下一个位置
                position = ++begin;
                // 指针1归位，重新指向根节点
                tempNode = rootNode;
            }else if (tempNode.isKeywordEnd()){
                // 发现敏感词，begin-position字符串替换掉
                sb.append(REPLACEMENT);
                // 进入下一个位置
                begin = ++ position;
                // 指针1归位，重新指向根节点
                tempNode = rootNode;
            }else { //在检测途中
                // 检查下一个字符
                if (position<text.length()-1){
                    position ++;
                }
            }
        }
        //将最后一批字符记入结果。即：3到终点，2没到终点，余下的字符不是敏感词的情况
        sb.append(text.substring(begin));
        return sb.toString();
    }

    // 判断是否为符号
    private boolean isSymbol(Character c){
        return !CharUtils.isAsciiAlphanumeric(c) && (c<0x2E80 || c>0x9FFF); //isAsciiAlphanumeric判断是否为普通字符。0x2E80-0x9FFF为东亚文字范围
    }

    //定义前缀树结构
    //只有这个类用到，所以定义为内部类，不允许外界访问
    private class TrieNode{

        //关键词结束标识
        private boolean isKeywordEnd = false;

        //当前节点的子节点，可能是多个，用map对其封装
        // key是下级节点的字符，value是下级节点
        Map<Character,TrieNode> subNodes =  new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        //添加子节点
        public void addSubNode(Character c, TrieNode node){
            subNodes.put(c,node);
        }
        //获取子节点
        public TrieNode getSubNode(Character c){
            return subNodes.get(c);
        }
    }


}
