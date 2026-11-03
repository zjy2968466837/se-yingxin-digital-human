package com.sicnu.yingxin.common.web.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 缓存值序列化器回归测试：DTO 含 {@link LocalDateTime} 时必须能写能读
 * （默认构造器不注册 JavaTimeModule，S5 实测踩过坑）。
 */
class RedisConfigTest {

    /** 故意不写成 record/final：默认类型信息只对非 final 类型生效。 */
    public static class Sample {
        private Long id;
        private LocalDateTime at;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public LocalDateTime getAt() {
            return at;
        }

        public void setAt(LocalDateTime at) {
            this.at = at;
        }
    }

    @Test
    void serializesAndDeserializesLocalDateTime() {
        GenericJackson2JsonRedisSerializer serializer = RedisConfig.jsonSerializer();
        Sample sample = new Sample();
        sample.setId(1L);
        sample.setAt(LocalDateTime.of(2026, 10, 27, 16, 30, 15, 123_000_000));

        Object restored = serializer.deserialize(serializer.serialize(sample));

        assertThat(restored).isInstanceOf(Sample.class);
        assertThat(((Sample) restored).getId()).isEqualTo(1L);
        assertThat(((Sample) restored).getAt()).isEqualTo(sample.getAt());
    }
}
