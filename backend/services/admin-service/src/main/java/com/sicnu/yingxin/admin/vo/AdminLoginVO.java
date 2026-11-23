package com.sicnu.yingxin.admin.vo;

/**
 * 管理员登录/注册结果：**不含令牌与会话 id**（会话 id 只进 HttpOnly Cookie，不落前端）。
 *
 * @param username  登录名
 * @param realName  姓名（后台右上角展示用）
 * @param role      粗粒度角色，当前固定 ADMIN；细分权限由 F9.2 加载
 * @param expiresIn 会话有效期秒数（与 Cookie Max-Age 一致）
 */
public record AdminLoginVO(String username, String realName, String role, long expiresIn) {
}
