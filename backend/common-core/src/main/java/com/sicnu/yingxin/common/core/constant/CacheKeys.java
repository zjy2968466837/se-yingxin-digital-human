package com.sicnu.yingxin.common.core.constant;

/**
 * 缓存 key 口径（开发规范 §5.4）：统一 {@code yx:<服务>:<业务>:<标识>}，全小写、禁止空格与中文。
 *
 * <p>三条硬约束：① 值一律 JSON 序列化（禁用 JDK 序列化）；② **key 必须设 TTL**，禁止永久键；
 * ③ 数据变更由写入方主动失效，缓存不得作为唯一数据源。
 */
public final class CacheKeys {

    /** 全局前缀 */
    public static final String PREFIX = "yx:";

    /** 会话：yx:auth:session:{tokenId}，TTL 与令牌一致（令牌失效即删） */
    public static final String AUTH_SESSION = PREFIX + "auth:session:";

    /** 问答热点头：yx:qa:hot:{questionHash}，命中即回，内容发布时失效 */
    public static final String QA_HOT = PREFIX + "qa:hot:";

    /** 限流计数：yx:<服务>:rate:{标识}，固定短 TTL */
    public static final String RATE_LIMIT = "%s:rate:%s";

    public static String authSession(String tokenId) {
        return AUTH_SESSION + tokenId;
    }

    public static String qaHot(String questionHash) {
        return QA_HOT + questionHash;
    }

    public static String rateLimit(String service, String identity) {
        return String.format(RATE_LIMIT, PREFIX + service, identity);
    }

    private CacheKeys() {
    }
}
