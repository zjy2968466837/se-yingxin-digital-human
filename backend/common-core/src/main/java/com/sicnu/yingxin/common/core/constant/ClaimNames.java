package com.sicnu.yingxin.common.core.constant;

/**
 * JWT 负载里的声明名（开发规范 §4.1）：负载**只放** {@code userId}、{@code role}、{@code exp}/{@code jti}，
 * 不放姓名、学号、身份证等隐私字段。
 *
 * <p>签发方（auth-service）与校验方（gateway）必须使用同一组名字，故放在公共内核里共享；
 * 其他服务既不签发也不解析令牌（它们只读网关注入的 {@code X-User-*} 头）。
 */
public final class ClaimNames {

    public static final String USER_ID = "userId";

    public static final String ROLE = "role";

    private ClaimNames() {
    }
}
