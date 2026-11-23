package com.sicnu.yingxin.auth.dto;

/**
 * 服务间创建会话请求（admin-service → auth-service）。
 *
 * <p>只传 {@code userId / role}，**不接受口令、手机号等任何凭证或隐私字段**。
 */
public record InternalSessionRequest(Long userId, String role) {
}
