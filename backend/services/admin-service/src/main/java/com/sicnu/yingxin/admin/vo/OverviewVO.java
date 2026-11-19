package com.sicnu.yingxin.admin.vo;

import java.util.List;

/**
 * 数据看板聚合结果（数据驾驶舱单次快照）。
 */
public record OverviewVO(
        String date,
        Kpi kpi,
        List<TrendPoint> trend,
        List<CollegeProgress> colleges,
        List<HotQuestion> hotQuestions,
        QaHit qaHit) {

    public record Kpi(
            long checkinCount,
            long totalStudents,
            double checkinRate,
            long qaCount,
            double cacheHitRate,
            long avgCostMs,
            long unanswered) {
    }

    public record TrendPoint(int hour, long count) {
    }

    public record CollegeProgress(String college, long total, long done, double rate) {
    }

    public record HotQuestion(String question, long hitCount) {
    }

    /** 问答命中分布：缓存 / 问答库 / 未命中 */
    public record QaHit(long cache, long lib, long miss) {
    }
}
