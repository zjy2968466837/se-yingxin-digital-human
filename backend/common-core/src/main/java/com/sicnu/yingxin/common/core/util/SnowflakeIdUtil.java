package com.sicnu.yingxin.common.core.util;

/**
 * 雪花 ID 简易生成器。
 *
 * <p>规范 §5.2 要求主键使用分布式/雪花 ID。本实现用于非 MyBatis-Plus 场景（如单元测试、网关本地生成 traceId 等），
 * 业务服务持久层建议直接使用 MyBatis-Plus 的 {@code IdWorker}。
 */
public class SnowflakeIdUtil {

    /** 起始时间戳：2026-01-01 00:00:00 */
    private static final long START_TIMESTAMP = 1767225600000L;

    /** 数据中心 ID 位数 */
    private static final long DATA_CENTER_BITS = 5L;

    /** 机器 ID 位数 */
    private static final long WORKER_BITS = 5L;

    /** 序列号位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 最大数据中心 ID */
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_BITS);

    /** 最大机器 ID */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_BITS);

    private static final long WORKER_SHIFT = SEQUENCE_BITS;
    private static final long DATA_CENTER_SHIFT = SEQUENCE_BITS + WORKER_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_BITS + DATA_CENTER_BITS;

    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private final long workerId;
    private final long dataCenterId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * 默认实例：workerId/dataCenterId 均取自环境变量，缺省为 0。
     * 生产环境须为每个实例分配不同 workerId，避免 ID 冲突。
     */
    public static final SnowflakeIdUtil INSTANCE = new SnowflakeIdUtil(
            parseEnv("SNOWFLAKE_WORKER_ID", 0),
            parseEnv("SNOWFLAKE_DATA_CENTER_ID", 0));

    public SnowflakeIdUtil(long workerId, long dataCenterId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId 必须在 0-" + MAX_WORKER_ID + " 之间");
        }
        if (dataCenterId < 0 || dataCenterId > MAX_DATA_CENTER_ID) {
            throw new IllegalArgumentException("dataCenterId 必须在 0-" + MAX_DATA_CENTER_ID + " 之间");
        }
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
    }

    public synchronized long nextId() {
        long timestamp = currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨，拒绝生成 ID");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (dataCenterId << DATA_CENTER_SHIFT)
                | (workerId << WORKER_SHIFT)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    private static int parseEnv(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
