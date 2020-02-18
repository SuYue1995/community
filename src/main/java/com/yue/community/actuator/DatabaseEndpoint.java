package com.yue.community.actuator;

import com.yue.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
@Endpoint(id = "database") // 端点取名，通过id访问端点
public class DatabaseEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseEndpoint.class);

    //尝试获取数据库连接：方法1.访问连接池，注入dataSource，getConnection获取连接；方法2. 注入连接参数，通过driverManager访问
    // 连接池Spring容器管理，直接注入即可
    @Autowired
    private DataSource dataSource;// DataSource是连接池的顶层接口

    @ReadOperation //表示该方法是通过GET请求访问；post请求注解为@WriteOperation
    public String checkConnection(){
        try(
                Connection conn = dataSource.getConnection(); // 小括号初始化的资源，编译时会自动添加finally关闭资源，不需手动关闭。
                ) {
            return CommunityUtil.getJSONString(0, "获取数据库连接成功");
        } catch (SQLException e) {
            logger.error("获取数据库连接失败" + e.getMessage());
            return CommunityUtil.getJSONString(1, "获取数据库连接失败");
        }
    }

}
