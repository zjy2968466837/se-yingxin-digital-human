package com.sicnu.yingxin.admin.vo;

/**
 * 图形验证码（data URL 直接给 img 用，前端无需再拼前缀）。
 *
 * @param captchaId      验证码标识，登录时原样回传
 * @param image          {@code data:image/png;base64,...}
 * @param expireInSeconds 有效期秒数
 */
public record CaptchaVO(String captchaId, String image, long expireInSeconds) {
}
