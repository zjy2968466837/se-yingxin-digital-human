package com.sicnu.yingxin.admin.vo;

/**
 * 当前登录管理员（GET /api/admin/auth/me）：前端路由守卫用它确认会话是否有效。
 *
 * @param userId  管理员 id（来自网关注入的 X-User-Id）
 * @param username 登录名
 * @param realName 姓名
 * @param role     角色（来自网关注入的 X-User-Role）
 */
public record AdminMeVO(Long userId, String username, String realName, String role) {
}
