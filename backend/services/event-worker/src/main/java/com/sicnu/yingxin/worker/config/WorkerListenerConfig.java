package com.sicnu.yingxin.worker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Worker 消费容器：手动 ack、prefetch 背压、单并发（多实例横向扩容时各起一个进程）。
 */
@Configuration
public class WorkerListenerConfig {

    /** Worker 无 web starter，Jackson 自动装配不生效，手动提供 ObjectMapper */
    @Bean
    public ObjectMapper workerObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    @Bean
    public SimpleMessageListenerContainer workerListenerContainer(ConnectionFactory connectionFactory,
                                                                  com.sicnu.yingxin.worker.consumer.EventWorker worker,
                                                                  WorkerProperties props) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(RabbitTopologyConfig.QUEUE_EVENTS);
        container.setMessageListener(worker);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setPrefetchCount(Math.max(props.getBatchSize(), 50));
        container.setConcurrentConsumers(1);
        return container;
    }
}
