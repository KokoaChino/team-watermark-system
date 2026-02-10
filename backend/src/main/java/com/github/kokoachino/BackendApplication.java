package com.github.kokoachino;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * 项目启动类
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Slf4j
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext context = SpringApplication.run(BackendApplication.class, args);
        Environment env = context.getEnvironment();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String activeProfile = String.join(",", env.getActiveProfiles());
        String appName = env.getProperty("spring.application.name", "team-watermark-system");
        String localUrl = "http://localhost:" + port + contextPath;
        String externalUrl = "http://" + ip + ":" + port + contextPath;
        String docUrl = externalUrl + "/doc.html";
        log.info("""

                ----------------------------------------------------------
                	协作式批量图片水印处理系统启动成功！
                ----------------------------------------------------------
                	应用名称: {}
                	运行环境: {}
                	本地访问: {}
                	外部访问: {}
                	API文档: {}
                ----------------------------------------------------------
                """,
                appName,
                activeProfile.isEmpty() ? "default" : activeProfile,
                localUrl,
                externalUrl,
                docUrl
        );
    }
}
