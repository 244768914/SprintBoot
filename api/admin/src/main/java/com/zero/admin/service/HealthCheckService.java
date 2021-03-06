package com.zero.admin.service;

import com.zaxxer.hikari.HikariDataSource;
import com.zero.admin.util.HttpClient;
import com.zero.admin.util.RedisHelper;
import com.zero.common.util.DateHelper;
import com.zero.common.vo.HealthCheckVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author yezhaoxing
 * @date 2017/11/02
 */
@Service
public class HealthCheckService {

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckService.class);
    @Resource
    private HikariDataSource masterDataSource;
    @Resource
    private RedisHelper<String, String> redisHelper;
    @Value("${spring.redis.host}")
    private String redisHost;
    @Value("${spring.redis.port}")
    private String redisPort;
    @Resource(name = "feiGeHttpClient")
    private HttpClient feiGeHttpClient;
    @Resource(name = "weChatHttpClient")
    private HttpClient weChatHttpClient;

    /**
     * 健康检查
     */
    public List<HealthCheckVo> healthCheck() {
        List<HealthCheckVo> healthCheckVos = new ArrayList<>();
        healthCheckVos.add(checkDBConnection());
        healthCheckVos.add(checkRedisConnection());
        healthCheckVos.add(feiGeHttpClient.healthCheck());
        healthCheckVos.add(weChatHttpClient.healthCheck());
        return healthCheckVos;
    }

    /**
     * 检查数据库连接是否正常
     */
    private HealthCheckVo checkDBConnection() {
        String url = masterDataSource.getJdbcUrl();
        String driver = masterDataSource.getDriverClassName();
        String user = masterDataSource.getUsername();
        String password = masterDataSource.getPassword();
        Connection conn = null;
        Statement statement = null;
        HealthCheckVo model = new HealthCheckVo();
        model.setServiceName(url);
        try {
            long e = System.currentTimeMillis();
            Class.forName(driver);
            conn = DriverManager.getConnection(url, user, password);
            statement = conn.createStatement();
            statement.executeQuery("select 1");
            model.setCostTime(String.format("%sms", System.currentTimeMillis() - e));
            model.setNormal(true);
        } catch (Exception e) {
            LOG.error("[checkDB]发生异常", e);
            model.setNormal(false);
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                LOG.error("[checkDB]关闭资源发生异常", e);
            }
        }
        return model;
    }

    private HealthCheckVo checkRedisConnection() {
        HealthCheckVo healthCheckVo = new HealthCheckVo();
        healthCheckVo.setServiceName(String.format("redis-->%s:%s", redisHost, redisPort));
        try {
            long startTimeMillis = System.currentTimeMillis();
            redisHelper.set(String.format("%scheckRedisConnection", ""),
                    DateHelper.format(new Date(startTimeMillis), "yyyy-MM-dd HH:mm:ss"));
            healthCheckVo.setNormal(true);
            healthCheckVo.setCostTime(String.format("%sms", System.currentTimeMillis() - startTimeMillis));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            healthCheckVo.setNormal(false);
        }
        return healthCheckVo;
    }
}
