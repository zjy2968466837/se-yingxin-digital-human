package com.sicnu.yingxin.qa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 智能问答：问答库、意图理解、多轮对话
 */
@SpringBootApplication
@MapperScan("com.sicnu.yingxin.qa.mapper")
public class QaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QaServiceApplication.class, args);
    }
}
