package com.sicnu.yingxin.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicnu.yingxin.admin.vo.OverviewVO;
import com.sicnu.yingxin.common.core.constant.CacheKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 看板聚合服务（参照 freshman-guide-engineering 的快照思路）。
 *
 * <p>缓存策略：Redis 维护版本号 {@link CacheKeys#DASH_VERSION}，事件 worker 每落一批就自增；
 * 快照按版本号做 key（{@link CacheKeys#DASH_SNAPSHOT} + version），TTL 60 秒兜底。
 * 读多写少 + 主动失效，大屏 5 秒轮询基本全部命中缓存，不重复全表聚合。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final long SNAPSHOT_TTL_SECONDS = 60;

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public OverviewVO overview() {
        String version = currentVersion();
        String cacheKey = CacheKeys.DASH_SNAPSHOT + version;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, OverviewVO.class);
            } catch (JsonProcessingException e) {
                log.warn("看板快照反序列化失败，重新聚合：{}", e.getMessage());
            }
        }
        OverviewVO snapshot = buildSnapshot();
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(snapshot),
                    Duration.ofSeconds(SNAPSHOT_TTL_SECONDS));
        } catch (JsonProcessingException e) {
            log.warn("看板快照序列化失败：{}", e.getMessage());
        }
        return snapshot;
    }

    private String currentVersion() {
        try {
            String version = redisTemplate.opsForValue().get(CacheKeys.DASH_VERSION);
            return version == null ? "0" : version;
        } catch (Exception e) {
            log.warn("读取看板版本号失败，跳过缓存：{}", e.getMessage());
            return "no-cache-" + System.currentTimeMillis();
        }
    }

    private OverviewVO buildSnapshot() {
        long totalStudents = queryLong(
                "SELECT COUNT(*) FROM auth_user WHERE deleted = 0 AND status = 1");
        long checkinCount = queryLong(
                "SELECT COUNT(DISTINCT user_id) FROM admin_behavior_event "
                        + "WHERE event_type = 3 AND DATE(event_time) = CURDATE() AND user_id IS NOT NULL");
        long qaCount = queryLong(
                "SELECT COUNT(*) FROM qa_ask_log WHERE DATE(create_time) = CURDATE()");
        long avgCostMs = queryLong(
                "SELECT COALESCE(AVG(cost_ms), 0) FROM qa_ask_log WHERE DATE(create_time) = CURDATE()");
        long unanswered = queryLong(
                "SELECT COUNT(*) FROM qa_unanswered WHERE status = 1 AND deleted = 0");

        Map<Integer, Long> hitTypes = new HashMap<>();
        jdbcTemplate.query(
                "SELECT hit_type, COUNT(*) FROM qa_ask_log WHERE DATE(create_time) = CURDATE() GROUP BY hit_type",
                rs -> {
                    hitTypes.put(rs.getInt(1), rs.getLong(2));
                });
        long cacheHits = hitTypes.getOrDefault(1, 0L);
        long libHits = hitTypes.getOrDefault(2, 0L);
        long misses = hitTypes.getOrDefault(4, 0L);
        long hitTotal = cacheHits + libHits + misses;
        double cacheHitRate = hitTotal == 0 ? 0 : (double) cacheHits / hitTotal;

        List<OverviewVO.TrendPoint> trend = new ArrayList<>();
        Map<Integer, Long> byHour = new HashMap<>();
        jdbcTemplate.query(
                "SELECT HOUR(event_time) h, COUNT(*) FROM admin_behavior_event "
                        + "WHERE event_type = 3 AND DATE(event_time) = CURDATE() GROUP BY HOUR(event_time)",
                rs -> {
                    byHour.put(rs.getInt(1), rs.getLong(2));
                });
        for (int hour = 0; hour < 24; hour++) {
            trend.add(new OverviewVO.TrendPoint(hour, byHour.getOrDefault(hour, 0L)));
        }

        List<OverviewVO.CollegeProgress> colleges = new ArrayList<>();
        jdbcTemplate.query(
                "SELECT ai.college, "
                        + "  COUNT(DISTINCT ai.user_id) AS total, "
                        + "  COUNT(DISTINCT CASE WHEN cp.done_cnt = "
                        + "    (SELECT COUNT(*) FROM checkin_step_template WHERE enabled = 1 AND deleted = 0) "
                        + "    THEN ai.user_id END) AS done "
                        + "FROM auth_identity ai "
                        + "LEFT JOIN (SELECT user_id, COUNT(*) AS done_cnt FROM checkin_progress "
                        + "  WHERE status = 2 AND deleted = 0 GROUP BY user_id) cp ON cp.user_id = ai.user_id "
                        + "WHERE ai.deleted = 0 "
                        + "GROUP BY ai.college ORDER BY ai.college",
                rs -> {
                    String college = rs.getString(1);
                    long total = rs.getLong(2);
                    long done = rs.getLong(3);
                    colleges.add(new OverviewVO.CollegeProgress(college, total, done,
                            total == 0 ? 0 : (double) done / total));
                });

        List<OverviewVO.HotQuestion> hotQuestions = new ArrayList<>();
        jdbcTemplate.query(
                "SELECT question, hit_count FROM qa_question WHERE status = 1 AND deleted = 0 "
                        + "ORDER BY hit_count DESC LIMIT 5",
                rs -> {
                    hotQuestions.add(new OverviewVO.HotQuestion(rs.getString(1), rs.getLong(2)));
                });

        double checkinRate = totalStudents == 0 ? 0 : (double) checkinCount / totalStudents;
        return new OverviewVO(
                LocalDate.now().toString(),
                new OverviewVO.Kpi(checkinCount, totalStudents, checkinRate,
                        qaCount, cacheHitRate, avgCostMs, unanswered),
                trend, colleges, hotQuestions,
                new OverviewVO.QaHit(cacheHits, libHits, misses));
    }

    private long queryLong(String sql) {
        try {
            Long value = jdbcTemplate.queryForObject(sql, Long.class);
            return value == null ? 0 : value;
        } catch (DataAccessException e) {
            log.warn("看板统计查询失败 sql 原因={}", e.getMessage());
            return 0;
        }
    }
}
