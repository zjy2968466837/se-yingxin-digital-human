package com.sicnu.yingxin.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 事件消费 Worker 独立进程。
 *
 * <p>把问答、打卡等埋点事件从主链路剥离：业务服务只管发 MQ，
 * 本进程攒批、事务落库、失效看板缓存；可多实例横向消费。
 */
@SpringBootApplication
@EnableScheduling
public class EventWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventWorkerApplication.class, args);
    }
}
