package com.github.kokoachino.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Knife4j 配置类
 *
 * @author kokoachino
 * @date 2026-02-03
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("批量图片水印协作平台 API")
                        .description("团队批量图片水印协作平台后端接口文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("kokoachino")
                                .email("2178740980@qq.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
