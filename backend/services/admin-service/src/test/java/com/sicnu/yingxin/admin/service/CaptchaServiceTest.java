package com.sicnu.yingxin.admin.service;

import com.sicnu.yingxin.admin.config.AdminAuthProperties;
import com.sicnu.yingxin.admin.constant.AdminErrorCodes;
import com.sicnu.yingxin.admin.vo.CaptchaVO;
import com.sicnu.yingxin.common.core.constant.CacheKeys;
import com.sicnu.yingxin.common.core.error.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证码服务单元测试：生成、一次性消费、过期与错误分支（Redis 全部 mock）。
 */
@ExtendWith(MockitoExtension.class)
class CaptchaServiceTest {

    @Mock
    StringRedisTemplate redis;

    @Mock
    ValueOperations<String, String> valueOps;

    private CaptchaService service;

    @BeforeEach
    void setUp() {
        AdminAuthProperties properties = new AdminAuthProperties();
        properties.setCaptchaTtl(Duration.ofMinutes(5));
        service = new CaptchaService(redis, properties);
    }

    @Test
    @DisplayName("生成：返回 data URL、写入 Redis 且带 TTL")
    void generate_storesCodeWithTtl() {
        when(redis.opsForValue()).thenReturn(valueOps);

        CaptchaVO captcha = service.generate();

        assertThat(captcha.captchaId()).hasSize(32);
        assertThat(captcha.image()).startsWith("data:image/png;base64,");
        assertThat(captcha.expireInSeconds()).isEqualTo(300);
        verify(valueOps).set(eq(CacheKeys.adminCaptcha(captcha.captchaId())), anyString(), eq(Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("校验通过：不抛异常，key 已被消费")
    void verify_success_consumesKey() {
        String key = CacheKeys.adminCaptcha("cid");
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(key)).thenReturn("AB34");

        service.verify("cid", "ab34");

        verify(redis).delete(key);
    }

    @Test
    @DisplayName("校验失败：2802；且无论对错都删除 key（防重放）")
    void verify_mismatch_deletesKeyAndThrows() {
        String key = CacheKeys.adminCaptcha("cid");
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(key)).thenReturn("AB34");

        assertThatThrownBy(() -> service.verify("cid", "ZZZZ"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(AdminErrorCodes.CAPTCHA_INVALID);
        verify(redis).delete(key);
    }

    @Test
    @DisplayName("已过期：2802")
    void verify_expired_throws() {
        String key = CacheKeys.adminCaptcha("cid");
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(key)).thenReturn(null);

        assertThatThrownBy(() -> service.verify("cid", "AB34"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(AdminErrorCodes.CAPTCHA_INVALID);
    }
}
