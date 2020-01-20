package com.yue.community.dao;

import com.yue.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Mapper //表示该类是一个数据访问的对象，需要容器来管理
@Deprecated // redis优化存储登录凭证，声明该组件不再使用
public interface LoginTicketMapper {

    @Insert({ //@Insert注解，将{}内的多个字符串拼成一条sql语句
            "insert into login_ticket(user_id,ticket,status,expired) ", //每一句话，一个引号内最后加一个空格
            "values(#{userId},#{ticket},#{status},#{expired})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id") //声明sql一些机制,自动生成主键，将生成的值注入给对象的keyProperty=id属性
    int insertLoginTicket(LoginTicket loginTicket); //返回影响的行数

    //查询方法，依据核心数据凭证ticket查询，最终要把ticket字符串发送给浏览器保存，其他数据是服务端自己存一份。
    //客户端通过cookie存储了ticket之后，再次访问服务器时，传ticket，服务器根据ticket查询到整条数据，得知哪个用户访问
    //ticket是唯一标识，所以只能查询到一条数据
    @Select({
            "select id, user_id, ticket, status, expired ",
            "from login_ticket where ticket=#{ticket}"
    })
    LoginTicket selectByTicket(String ticket);

    //修改凭证的状态。退出后，凭证失效，修改凭证状态为失效，而不是删除数据。
    @Update({
            "<script>",
            "update login_ticket set status=#{status} where ticket=#{ticket} ",
            "<if test=\"ticket!=null\">",
            "and 1=1",
            "</if>",
            "</script>",
    })
    int updateStatus(String ticket, int status);

}
