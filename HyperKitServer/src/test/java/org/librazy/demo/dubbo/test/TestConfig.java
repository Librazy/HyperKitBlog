package org.librazy.demo.dubbo.test;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class TestConfig {
    @Bean
    RestTemplateBuilder restTemplateBuilder(){
        return new RestTemplateBuilder().requestFactory(() -> {
            SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
            simpleClientHttpRequestFactory.setOutputStreaming(false);
            return simpleClientHttpRequestFactory;
        });
    }
}
