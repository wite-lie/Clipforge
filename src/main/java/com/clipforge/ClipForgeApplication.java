package com.clipforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ClipForgeApplication（创作规划智能体应用启动类）。
 *
 * <p>该类是 Spring Boot（Spring Boot 框架）应用的入口，负责启动整个后端服务。
 */
@SpringBootApplication
@EnableScheduling
public class ClipForgeApplication {

    /**
     * main（主函数）是 Java（爪哇语言）程序启动入口。
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        // 调用 SpringApplication（Spring 应用启动器）启动 ClipForge Agent（创作规划智能体）应用。
        SpringApplication.run(ClipForgeApplication.class, args);
    }
}
