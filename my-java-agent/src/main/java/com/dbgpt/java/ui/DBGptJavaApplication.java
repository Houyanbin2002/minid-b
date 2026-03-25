package com.dbgpt.java.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 对应了 DB-GPT 的 dbgpt-app/src/dbgpt_app/app.py 或者 pilot/server/dbgpt_server.py
 * 系统入口主程序，加载所有的组件（Agent、Schema、Retrievers、Redis内存等）
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.dbgpt.java.fitness", "com.dbgpt.java.core", "com.dbgpt.java.ui"})
public class DBGptJavaApplication {
    public static void main(String[] args) {
        SpringApplication.run(DBGptJavaApplication.class, args);
        System.out.println("=============== ?? DB-GPT (Java Spring Boot) Fitness Multi-Agent Engine Started! ?? ===============");
    }
}