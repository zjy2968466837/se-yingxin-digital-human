package com.sicnu.yingxin.auth.vo;

/**
 * 会话创建结果（服务间内部接口用）。sessionId 由调用方写入 HttpOnly Cookie。
 *
 * @param sessionId 不透明会话 id（64 位十六进制）
 * @param expiresIn 有效期秒数（也是 Cookie Max-Age）
 */
public record SessionVO(String sessionId, long expiresIn) {
}
