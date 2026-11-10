package com.sicnu.yingxin.admin.vo;

/**
 * 管理员登录结果：令牌只放前端存储，**不回传任何口令或散列**。
 *
 * @param token     访问令牌（JWT，网关校验签名 + Redis 会话）
 * @param expiresIn 有效期秒数
 * @param username  登录名
 * @param realName  姓名（后台右上角展示用）
 */
public record AdminLoginVO(String token, long expiresIn, String username, String realName) {
}
