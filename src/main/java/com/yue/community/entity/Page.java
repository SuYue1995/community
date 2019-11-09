package com.yue.community.entity;
/*
 * 封装分页相关的信息
 */
public class Page {

    //利用对象让服务端接受页面传入的信息，先解决页面传入信息的接收问题
    //当前的页码，默认为1
    private int current = 1;

    //显示上限，最多显示多少条数据，默认为10
    private int limit = 10;

    //数据总数，服务端查询得到，用于计算总页数（数据总数/显示上限）
    private int rows;

    //查询路径，每一页都对应一个路径，用来复用分页连接
    private String path;

    //传入数据时，需要做判断，避免传入错误的数据

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        if (current >= 1){
            this.current = current;
        }
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        if (limit >= 1 && limit <= 100){ //过量的数据会导致页面卡死，影响用户体验
            this.limit = limit;
        }
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        if (rows >= 0){
            this.rows = rows;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    //数据库（查询），页面（显示）
    public int getOffset(){ //需要当前页的起始行，需要当前页的页面算出起始行
        //current * limit - limit
        return (current - 1) * limit;
    }

    //用来获取总页数，显示页码，页码范围不能超过一共可能出现的页数
    public int getTotal(){
        //rows / limit [+1] (不能整除就多一页)
        if (rows % limit == 0){
            return rows/limit;
        }else {
            return rows/limit + 1;
        }
    }

    //一般只显示当前页的前两页和后两页，因此需要根据当前页计算，起始页和结束页是多少
    //获取起始页码
    public int getFrom(){
        int from = current - 2;
        return from < 1 ? 1 : from;
    }

    //获取结束页码
    public int getTo(){
        int to = current + 2;
        int total = getTotal();
        return to > total ? total : to;
    }
}
