package org.librazy.demo.dubbo;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.librazy.demo.dubbo.config.SecurityInstanceUtils;
import org.librazy.demo.dubbo.web.SrpController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Spring Boot 入口类
 */
@EnableDubboConfiguration
@SpringBootApplication
@EnableTransactionManagement
public class ServerApplication extends SpringBootServletInitializer {

    private static Logger log = LoggerFactory.getLogger(ServerApplication.class);

    static {
        try {
            SecurityInstanceUtils.setStrongRandom(SecureRandom.getInstanceStrong());
        } catch (NoSuchAlgorithmException e) {
            log.error("No secure random support available, ", e);
            Runtime.getRuntime().exit(1);
        }
    }

    /**
     * 标准Jar入口点
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
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
        return application.sources(ServerApplication.class);
    }

}