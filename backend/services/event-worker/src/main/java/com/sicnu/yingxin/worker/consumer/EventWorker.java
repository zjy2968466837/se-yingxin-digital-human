package com.sicnu.yingxin.worker.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.sicnu.yingxin.common.core.constant.CacheKeys;
import com.sicnu.yingxin.worker.config.RabbitTopologyConfig;
import com.sicnu.yingxin.worker.config.WorkerProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事件消费核心：攒批 → 事务批量落库 → 整批 ack → 失效看板缓存。
 *
 * <p>失败处理：整批失败后逐条重试，重试计数记录在消息头 x-retry；
 * 超过上限的消息经死信交换机进入 yingxin.events.dlq，供人工排查。
 * JSON 解析失败的消息不重试，直接进死信队列。
 */
@Slf4j
@Component
public class EventWorker implements ChannelAwareMessageListener {

    private static final String HEADER_RETRY = "x-retry";

    private final WorkerProperties props;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private final LinkedBlockingQueue<PendingMessage> pending = new LinkedBlockingQueue<>();
    private final AtomicLong idSeq = new AtomicLong();

    /** 满批冲刷锁，listener 线程与定时冲刷线程共用 */
    private final Object flushLock = new Object();

    public EventWorker(WorkerProperties props,
                       JdbcTemplate jdbcTemplate,
                       TransactionTemplate transactionTemplate,
                       StringRedisTemplate redisTemplate,
                       RabbitTemplate rabbitTemplate,
                       ObjectMapper objectMapper) {
        this.props = props;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        int retry = retryCount(message);

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(message.getBody(), new TypeReference<>() {
            });
        } catch (IOException e) {
            log.warn("消息 JSON 解析失败，直接进入死信队列 deliveryTag={}", deliveryTag);
            toDlq(message, deliveryTag, channel, "json_parse_error");
            return;
        }

        pending.offer(new PendingMessage(payload, message.getBody(), deliveryTag, channel, retry));
        if (pending.size() >= props.getBatchSize()) {
            flush();
        }
    }

    /** 定时兜底冲刷（由调度器调用）：未满批的消息按时落库 */
    @Scheduled(fixedDelayString = "${yingxin.worker.flush-ms:200}")
    public void scheduledFlush() {
        flush();
    }

    /** 优雅停机：冲刷残余消息 */
    @PreDestroy
    public void shutdownFlush() {
        flush();
    }

    private void flush() {
        synchronized (flushLock) {
            List<PendingMessage> items = new ArrayList<>();
            pending.drainTo(items);
            if (items.isEmpty()) {
                return;
            }
            try {
                int inserted = transactionTemplate.execute(status -> insertBatch(items));
                ackAll(items);
                bumpDashVersion();
                log.info("批次落库完成 rows={} inserted={}", items.size(), inserted);
            } catch (Exception batchError) {
                log.warn("批量落库失败，转为逐条重试 rows={} reason={}", items.size(), batchError.getMessage());
                for (PendingMessage item : items) {
                    retrySingle(item);
                }
            }
        }
    }

    /** 单事务批量插入；按 eventId 幂等去重 */
    private int insertBatch(List<PendingMessage> items) {
        Set<String> seenEventIds = new HashSet<>();
        List<Object[]> rows = new ArrayList<>();
        for (PendingMessage item : items) {
            String eventId = str(item.payload().get("eventId"));
            if (eventId != null && !seenEventIds.add(eventId)) {
                continue;
            }
            rows.add(new Object[]{
                    nextId(),
                    intOf(item.payload().get("eventType"), 5),
                    longOf(item.payload().get("userId")),
                    str(item.payload().get("service")),
                    longOf(item.payload().get("refId")),
                    intOf(item.payload().get("channel"), 1),
                    toJson(item.payload().get("ext"))
            });
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO admin_behavior_event (id, event_type, user_id, service, ref_id, channel, ext, event_time, "
                        + "create_time, update_time, create_by, update_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(3), NOW(3), NOW(3), 0, 0, 0)",
                rows);
        return rows.size();
    }

    private void retrySingle(PendingMessage item) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO admin_behavior_event (id, event_type, user_id, service, ref_id, channel, ext, event_time, "
                            + "create_time, update_time, create_by, update_by, deleted) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(3), NOW(3), NOW(3), 0, 0, 0)",
                    nextId(),
                    intOf(item.payload().get("eventType"), 5),
                    longOf(item.payload().get("userId")),
                    str(item.payload().get("service")),
                    longOf(item.payload().get("refId")),
                    intOf(item.payload().get("channel"), 1),
                    toJson(item.payload().get("ext")));
            ack(item);
            bumpDashVersion();
        } catch (Exception singleError) {
            handleFailure(item, singleError);
        }
    }

    private void handleFailure(PendingMessage item, Exception cause) {
        if (item.retry() < props.getRetryLimit()) {
            try {
                byte[] body = item.rawBody();
                rabbitTemplate.send("", RabbitTopologyConfig.QUEUE_EVENTS,
                        new Message(body, rebuildProperties(item.retry() + 1)));
                ack(item);
                log.info("消息重新入队 retry={} deliveryTag={} reason={}",
                        item.retry() + 1, item.deliveryTag(), cause.getMessage());
                return;
            } catch (Exception republishError) {
                log.error("重新入队失败 deliveryTag={} reason={}", item.deliveryTag(), republishError.getMessage());
            }
        }
        try {
            Message dead = new Message(item.rawBody(), rebuildProperties(item.retry()));
            dead.getMessageProperties().setHeader("x-death-reason", String.valueOf(cause.getMessage()));
            rabbitTemplate.send(RabbitTopologyConfig.DLX_EXCHANGE, RabbitTopologyConfig.QUEUE_DLQ, dead);
            ack(item);
            log.warn("消息进入死信队列 deliveryTag={} reason={}", item.deliveryTag(), cause.getMessage());
        } catch (Exception dlqError) {
            log.error("死信投递失败， nack 拒绝 deliveryTag={} reason={}", item.deliveryTag(), dlqError.getMessage());
            nack(item, false);
        }
    }

    private void toDlq(Message message, long deliveryTag, Channel channel, String reason) {
        try {
            Message dead = new Message(message.getBody(), message.getMessageProperties());
            dead.getMessageProperties().setHeader("x-death-reason", reason);
            rabbitTemplate.send(RabbitTopologyConfig.DLX_EXCHANGE, RabbitTopologyConfig.QUEUE_DLQ, dead);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ignored) {
                log.error("死信处理失败 deliveryTag={}", deliveryTag);
            }
        }
    }

    private MessageProperties rebuildProperties(int retry) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
        properties.setHeader(HEADER_RETRY, retry);
        return properties;
    }

    private int retryCount(Message message) {
        Object value = message.getMessageProperties().getHeader(HEADER_RETRY);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private void ackAll(List<PendingMessage> items) {
        for (PendingMessage item : items) {
            ack(item);
        }
    }

    private void ack(PendingMessage item) {
        try {
            item.channel().basicAck(item.deliveryTag(), false);
        } catch (IOException e) {
            log.error("ack 失败 deliveryTag={} reason={}", item.deliveryTag(), e.getMessage());
        }
    }

    private void nack(PendingMessage item, boolean requeue) {
        try {
            item.channel().basicNack(item.deliveryTag(), false, requeue);
        } catch (IOException e) {
            log.error("nack 失败 deliveryTag={}", item.deliveryTag());
        }
    }

    private void bumpDashVersion() {
        try {
            redisTemplate.opsForValue().increment(CacheKeys.DASH_VERSION);
        } catch (Exception e) {
            log.warn("看板版本号自增失败：{}", e.getMessage());
        }
    }

    private long nextId() {
        return (System.currentTimeMillis() << 12) | (idSeq.incrementAndGet() & 0xFFF);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String str(Object value) {
        return value == null ? null : value.toString();
    }

    private Long longOf(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer intOf(Object value, int defaultValue) {
        Long result = longOf(value);
        return result == null ? defaultValue : result.intValue();
    }

    /** 待处理消息：payload、原始字节（重投用）、投递标签与通道 */
    record PendingMessage(Map<String, Object> payload, byte[] rawBody, long deliveryTag,
                          Channel channel, int retry) {
    }
}
