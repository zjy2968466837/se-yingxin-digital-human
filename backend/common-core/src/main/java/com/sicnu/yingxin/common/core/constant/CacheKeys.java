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

    /** 登录失败计数：yx:auth:fail:{account}，固定短 TTL（10 分钟窗口） */
    public static final String AUTH_FAIL = PREFIX + "auth:fail:";

    /** 登录锁定：yx:auth:lock:{account}，固定短 TTL（10 分钟） */
    public static final String AUTH_LOCK = PREFIX + "auth:lock:";

    /** 问答热点头：yx:qa:hot:{questionHash}，命中即回，内容发布时失效 */
    public static final String QA_HOT = PREFIX + "qa:hot:";

    /** 限流计数：yx:<服务>:rate:{标识}，固定短 TTL */
    public static final String RATE_LIMIT = "%s:rate:%s";

    /** 数据看板聚合版本号：yx:admin:dash:version，事件落库即 INCR 主动失效快照 */
    public static final String DASH_VERSION = PREFIX + "admin:dash:version";

    /** 数据看板聚合快照：yx:admin:dash:snapshot:{version}，TTL 兜底 */
    public static final String DASH_SNAPSHOT = PREFIX + "admin:dash:snapshot:";

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
