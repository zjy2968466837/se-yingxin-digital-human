package com.sicnu.yingxin.checkin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 报到流程：步骤引导、状态查询、待办推送
 */
@SpringBootApplication
@MapperScan("com.sicnu.yingxin.checkin.mapper")
public class CheckinServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckinServiceApplication.class, args);
    }
}
