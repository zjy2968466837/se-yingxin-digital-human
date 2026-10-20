package com.sicnu.yingxin.auth.config;

/**
 * 认证服务 Redis Key 模板（规范 §5.4）。
 */
public final class AuthRedisKey {

    private AuthRedisKey() {
    }

    /** 会话令牌映射：yx:auth:session:<jti> → userId/role */
    public static final String SESSION = "yx:auth:session:%s";

    public static String session(String jti) {
        return String.format(SESSION, jti);
    }
}
