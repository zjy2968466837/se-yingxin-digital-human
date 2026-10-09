package com.sicnu.yingxin.common.core.error;

/**
 * 服务专属错误码段位（开发规范 §3.3）。
 *
 * <p>用于自检：{@code ServiceSegment.QA.contains(2203)} 为真才允许抛这个码，防止越段使用别人的码。
 * 段位与服务的对应关系是硬约定，新增服务必须先改《开发规范》再改这里。
 */
public enum ServiceSegment {

    AUTH(2101, 2199, "auth-service", "FR-1 身份核验与个性化迎新"),
    QA(2201, 2299, "qa-service", "FR-3 智能问答"),
    CHECKIN(2301, 2399, "checkin-service", "FR-2 报到全流程导引"),
    NAVIGATION(2401, 2499, "navigation-service", "FR-4 校园导航"),
    AID(2501, 2599, "aid-service", "FR-5 资助与绿色通道"),
    LIFE(2601, 2699, "life-service", "FR-6 生活服务聚合"),
    CONTENT(2701, 2799, "content-service", "FR-7 / FR-8 安全教育与入学适应"),
    ADMIN(2801, 2899, "admin-service", "FR-9 管理后台");

    private final int from;
    private final int to;
    private final String service;
    private final String scope;

    ServiceSegment(int from, int to, String service, String scope) {
        this.from = from;
        this.to = to;
        this.service = service;
        this.scope = scope;
    }

    public boolean contains(int code) {
        return code >= from && code <= to;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public String getService() {
        return service;
    }

    public String getScope() {
        return scope;
    }
}
