package org.librazy.demo.dubbo;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring Boot 入口类
 */
@EnableDubboConfiguration
@SpringBootApplication
@EnableTransactionManagement
public class ProviderApplication extends SpringBootServletInitializer {

    /**
     * 标准Jar入口点
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }

    /**
     * WAR入口点配置
     *
     * @param application SpringApplicationBuilder
     * @return SpringApplicationBuilder
     * @see <a href="https://docs.spring.io/spring-boot/docs/current/reference/html/howto-traditional-deployment.html">Spring Boot Reference Guide Part IX. ‘How-to’ guides	 85. Traditional deployment</a>
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ProviderApplication.class);
    }

}