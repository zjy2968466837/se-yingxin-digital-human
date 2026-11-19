package com.sicnu.yingxin.worker.config;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Worker 侧拓扑声明，参数必须与 common-web 的 MqTopologyConfiguration 完全一致。
 */
@Configuration
public class RabbitTopologyConfig {

    public static final String QUEUE_EVENTS = "yingxin.events";
    public static final String DLX_EXCHANGE = "yingxin.dlx";
    public static final String QUEUE_DLQ = "yingxin.events.dlq";

    @Bean
    public Declarables workerMqTopology() {
        Queue events = QueueBuilder.durable(QUEUE_EVENTS)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_DLQ)
                .build();
        DirectExchange dlx = new DirectExchange(DLX_EXCHANGE, true, false);
        Queue dlq = QueueBuilder.durable(QUEUE_DLQ).build();
        return new Declarables(events, dlx, dlq);
    }
}
