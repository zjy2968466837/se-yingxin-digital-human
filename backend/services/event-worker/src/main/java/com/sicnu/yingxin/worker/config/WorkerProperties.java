package com.sicnu.yingxin.worker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Worker 攒批参数。
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "yingxin.worker")
public class WorkerProperties {

    /** 满批条数 */
    private int batchSize = 100;

    /** 冲刷间隔毫秒 */
    private long flushMs = 200;

    /** 重试上限，超过进死信队列 */
    private int retryLimit = 3;
}
