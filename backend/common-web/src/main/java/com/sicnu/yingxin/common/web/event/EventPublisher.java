package com.sicnu.yingxin.common.web.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 统一事件发布器（横向能力 H-11）。
 *
 * <p>业务服务统一通过本类发布埋点/业务事件：首选 RabbitMQ（basicPublish +
 * waitForConfirmsOrDie 兜底校验）；MQ 不可用时自动降级为同步写 MySQL 事件表
 * （admin_behavior_event），保证看板数据不丢。
 *
 * <p>发布失败最多记录 WARN 日志，绝不向上抛异常——埋点是旁路逻辑，
 * 不能因为「记录失败」打断「报到打卡」这类主流程。
 */
@Slf4j
public class EventPublisher {

    /** 主事件队列名，与 event-worker 的声明保持一致，改一处必须同步另一处 */
    public static final String QUEUE_EVENTS = "yingxin.events";

    private static final AtomicLong FALLBACK_SEQ = new AtomicLong();

    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;
    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;

    public EventPublisher(ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
                          ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
                          ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                          ObjectMapper objectMapper) {
        this.rabbitTemplateProvider = rabbitTemplateProvider;
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * 发布业务事件。
     *
     * @param eventType 事件类型：1 问答 / 2 导航 / 3 报到打卡 / 4 内容阅读 / 5 页面访问
     * @param userId    触发用户，匿名可空
     * @param service   来源服务名，如 qa / checkin
     * @param refId     业务对象 ID，可空
     * @param channel   渠道：1 学生端网页 / 2 管理后台
     * @param ext       附加维度（如 hitType、stepCode），可空
     */
    public void publish(int eventType, Long userId, String service, Long refId, Integer channel,
                        Map<String, Object> ext) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", UUID.randomUUID().toString().replace("-", ""));
        payload.put("eventType", eventType);
        payload.put("userId", userId);
        payload.put("service", service);
        payload.put("refId", refId);
        payload.put("channel", channel);
        payload.put("ext", ext);

        try {
            RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
            if (rabbitTemplate != null) {
                publishViaMq(rabbitTemplate, payload);
                return;
            }
        } catch (Exception e) {
            log.warn("MQ 发布失败，降级同步写库 service={} eventType={} reason={}", service, eventType, e.getMessage());
        }

        fallbackWrite(eventType, userId, service, refId, channel, ext);
    }

    /** publisher confirm：waitForConfirmsOrDie 超时视为发送失败 */
    private void publishViaMq(RabbitTemplate rabbitTemplate, Map<String, Object> payload) throws Exception {
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .deliveryMode(2)
                .build();
        byte[] body = objectMapper.writeValueAsBytes(payload);
        rabbitTemplate.execute(channel -> {
            channel.basicPublish("", QUEUE_EVENTS, properties, body);
            channel.waitForConfirmsOrDie(1_000);
            return null;
        });
    }

    /** 降级路径：直接写 admin_behavior_event，并尽力自增看板版本号 */
    private void fallbackWrite(int eventType, Long userId, String service, Long refId, Integer channel,
                               Map<String, Object> ext) {
        try {
            JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
            if (jdbcTemplate == null) {
                log.warn("MQ 与数据源均不可用，事件丢弃 service={} eventType={}", service, eventType);
                return;
            }
            jdbcTemplate.update(
                    "INSERT INTO admin_behavior_event (id, event_type, user_id, service, ref_id, channel, ext, "
                            + "event_time, create_time, update_time, create_by, update_by, deleted) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(3), NOW(3), NOW(3), 0, 0, 0)",
                    nextFallbackId(), eventType, userId, service, refId, channel, toJson(ext));
            bumpDashVersion();
        } catch (Exception e) {
            log.warn("事件降级写库失败 service={} eventType={} reason={}", service, eventType, e.getMessage());
        }
    }

    private String toJson(Map<String, Object> ext) {
        if (ext == null || ext.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(ext);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void bumpDashVersion() {
        try {
            StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
            if (redis != null) {
                redis.opsForValue().increment(
                        com.sicnu.yingxin.common.core.constant.CacheKeys.DASH_VERSION);
            }
        } catch (Exception ignored) {
            // 缓存失效失败只影响看板刷新速度，忽略
        }
    }

    /** 降级路径的简易趋势递增 ID：时间戳左移 12 位 + 进程序号，演示规模下足够防撞 */
    private long nextFallbackId() {
        return (System.currentTimeMillis() << 12) | (FALLBACK_SEQ.incrementAndGet() & 0xFFF);
    }
}
