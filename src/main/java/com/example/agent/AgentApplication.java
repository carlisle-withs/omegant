package com.example.agent;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AgentApplication {

    private static final Logger log = LoggerFactory.getLogger(AgentApplication.class);

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(AgentApplication.class, args);
    }

    private static void loadDotenv() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();
            dotenv.entries().forEach(e ->
                    System.setProperty(e.getKey(), e.getValue())
            );
            log.info("已加载 .env 环境变量文件");
        } catch (Exception e) {
            log.warn("未找到或无法加载 .env 文件，将使用系统环境变量: {}", e.getMessage());
        }
    }
}
