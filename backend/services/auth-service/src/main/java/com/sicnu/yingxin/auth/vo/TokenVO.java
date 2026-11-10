package com.sicnu.yingxin.auth.vo;

/**
 * 令牌签发结果（服务间内部接口用）。
 *
 * @param token     访问令牌
 * @param expiresIn 有效期秒数
 */
public record TokenVO(String token, long expiresIn) {
}
