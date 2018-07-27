package org.librazy.demo.dubbo;

import com.google.common.base.Predicates;
import org.librazy.demo.dubbo.config.SecurityInstanceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.Errors;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Spring Boot 入口类
 */
//@EnableDubboConfiguration
@SpringBootApplication
@EnableSwagger2
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

    @SuppressWarnings("Guava")
    @Bean
    public Docket hyperKitApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                       .ignoredParameterTypes(Errors.class)
                       .select()
                       .apis(RequestHandlerSelectors.any())
                       .paths(Predicates.not(Predicates.or(PathSelectors.regex("/error.*"), PathSelectors.regex("/actuator.*"))))
                       .build()
                       .useDefaultResponseMessages(false)
                       .pathMapping("/")
                       .enableUrlTemplating(true);
    }
}