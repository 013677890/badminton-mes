package com.badminton.mes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 羽毛球 MES 系统启动类。
 *
 * <p>模块代码统一放在 {@code com.badminton.mes.module} 下，公共基础设施放在
 * {@code com.badminton.mes.common} 下，分层规则见 wiki/project-tech-stack.md。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@SpringBootApplication
@EnableScheduling
public class MesApplication {

    public static void main(String[] args) {
        // 将命令行参数交给 Spring Boot，完成组件扫描、自动配置以及定时任务基础设施初始化。
        SpringApplication.run(MesApplication.class, args);
    }
}
