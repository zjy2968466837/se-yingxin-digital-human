package com.sicnu.yingxin.admin.service;

import com.sicnu.yingxin.admin.config.AdminAuthProperties;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.constant.CacheKeys;
import com.sicnu.yingxin.admin.constant.AdminErrorCodes;
import com.sicnu.yingxin.admin.vo.CaptchaVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * 图形验证码（FR-9.1 / NFR-4.5）：服务端生成、Redis 存储、**一次性消费**。
 *
 * <p>三个要点：
 * ① 校验前先删除 key——无论对错都作废，防止同一个验证码被反复试；
 * ② 混淆字符集去掉 0/O/1/I，避免"看着对却总错"；
 * ③ 比对不区分大小写，但保留 4 位长度约束。
 */
@Service
public class CaptchaService {

    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 4;
    private static final int WIDTH = 132;
    private static final int HEIGHT = 44;

    private final StringRedisTemplate redis;
    private final AdminAuthProperties properties;
    private final SecureRandom random = new SecureRandom();

    public CaptchaService(StringRedisTemplate redis, AdminAuthProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public CaptchaVO generate() {
        String code = randomCode();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(CacheKeys.adminCaptcha(captchaId), code, properties.getCaptchaTtl());
        return new CaptchaVO(captchaId, "data:image/png;base64," + render(code),
                properties.getCaptchaTtl().toSeconds());
    }

    /** 校验并销毁验证码；失败抛 2802。 */
    public void verify(String captchaId, String input) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(input)) {
            throw new BizException(AdminErrorCodes.CAPTCHA_INVALID, "请输入验证码");
        }
        String key = CacheKeys.adminCaptcha(captchaId);
        String expected = redis.opsForValue().get(key);
        redis.delete(key);
        if (expected == null) {
            throw new BizException(AdminErrorCodes.CAPTCHA_INVALID, "验证码已过期，请刷新后重试");
        }
        if (!expected.equalsIgnoreCase(input.trim())) {
            throw new BizException(AdminErrorCodes.CAPTCHA_INVALID, "验证码错误，请重新输入");
        }
    }

    private String randomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return code.toString();
    }

    private String render(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(246, 248, 250));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线：压低对比度，不遮住字符
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 6; i++) {
                g.setColor(randomColor(170, 220));
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }

            int x = 14;
            for (char c : code.toCharArray()) {
                g.setFont(new Font("SansSerif", Font.BOLD, 26 + random.nextInt(4)));
                g.setColor(randomColor(30, 120));
                double angle = (random.nextDouble() - 0.5) * 0.45;
                g.rotate(angle, x + 12.0, HEIGHT / 2.0);
                g.drawString(String.valueOf(c), x, 31);
                g.rotate(-angle, x + 12.0, HEIGHT / 2.0);
                x += 29;
            }

            // 噪点
            for (int i = 0; i < 90; i++) {
                g.setColor(randomColor(150, 230));
                g.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 1, 1);
            }
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("验证码生成失败", e);
        }
    }

    private Color randomColor(int min, int max) {
        int bound = max - min;
        return new Color(min + random.nextInt(bound), min + random.nextInt(bound), min + random.nextInt(bound));
    }
}
