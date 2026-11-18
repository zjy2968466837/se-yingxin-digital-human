package com.sicnu.yingxin.common.web.event;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * RabbitMQ 拓扑声明（事件队列 + 死信队列），自动装配。
 *
 * <p>声明幂等：队列参数必须与 event-worker 侧完全一致，否则声明报错。
 * 发布方服务启动时顺带完成拓扑声明，worker 未启动也不丢消息。
 *
 * <p>死信链路：主队列 yingxin.events 消费失败重试超限的消息，
 * 经死信交换机 yingxin.dlx 路由到 yingxin.events.dlq，供人工排查。
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
public class MqTopologyConfiguration {

    /** 死信交换机：消费失败重试超限的消息最终落到这里 */
    public static final String DLX_EXCHANGE = "yingxin.dlx";

    /** 死信队列 */
    public static final String QUEUE_DLQ = "yingxin.events.dlq";

    @Bean
    public Declarables yingxinMqTopology() {
        Queue events = QueueBuilder.durable(EventPublisher.QUEUE_EVENTS)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_DLQ)
                .build();
        DirectExchange dlx = new DirectExchange(DLX_EXCHANGE, true, false);
        Queue dlq = QueueBuilder.durable(QUEUE_DLQ).build();
        return new Declarables(events, dlx, dlq);
    }
}
