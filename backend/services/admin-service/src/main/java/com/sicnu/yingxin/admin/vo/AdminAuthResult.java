package com.sicnu.yingxin.admin.vo;

/**
 * 登录/注册的服务层结果：用户信息 + 会话 id（会话 id 只用于写 HttpOnly Cookie）。
 */
public record AdminAuthResult(AdminLoginVO user, String sessionId) {
}
