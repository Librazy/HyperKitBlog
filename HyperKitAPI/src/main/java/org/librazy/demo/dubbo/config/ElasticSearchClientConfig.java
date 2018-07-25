package org.librazy.demo.dubbo.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class ElasticSearchClientConfig {

    @Value("${es.host}")
    private String host;

    @Value("${es.port}")
    private int port;

    @Value("${es.scheme}")
    private String scheme;

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public RestHighLevelClient restHighLevelClientBean() {
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(host, port, scheme)));
    }
}
