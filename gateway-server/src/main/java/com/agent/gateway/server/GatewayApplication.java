package com.agent.gateway.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootApplication
@Slf4j
public class GatewayApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(GatewayApplication.class, args);
        Environment env = context.getEnvironment();
        String port = env.getProperty("server.port", "8080");
        String path = env.getProperty("server.servlet.context-path", "");

        log.info("\n----------------------------------------------------------\n\t" +
                "Sub-Agent Gateway 启动成功! 访问链接如下:\n\t" +
                "本地控制台: \thttp://localhost:{}{}\n\t" +
                "Swagger 文档: \thttp://localhost:{}{}/swagger-ui.html\n\t" +
                "H2 控制台: \thttp://localhost:{}{}/h2-console\n" +
                "----------------------------------------------------------",
                port, path, port, path, port, path);
    }
}
