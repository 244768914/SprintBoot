package com.zero.customer.bean;

import com.zero.customer.util.HttpClient;
import com.zero.customer.vo.http.properties.HttpFeiGeProperties;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * http bean对象
 *
 * @author yezhaoxing
 * @date 2017/10/17
 */
@Configuration
@Data
public class HttpClientBean {

    @Resource
    private HttpFeiGeProperties httpFeiGeProperties;

    @Bean("feiGeHttpClient")
    public HttpClient localHttpClient() {
        return new HttpClient(httpFeiGeProperties.getScheme(), httpFeiGeProperties.getHostname(),
                Integer.valueOf(httpFeiGeProperties.getPort()));
    }
}
