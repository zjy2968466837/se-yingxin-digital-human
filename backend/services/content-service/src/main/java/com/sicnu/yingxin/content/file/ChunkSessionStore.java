package com.sicnu.yingxin.content.file;

import java.time.Duration;
import java.util.Set;

/**
 * 分片会话存储抽象：Redis 实现用于运行，内存实现用于单测（不连真实 Redis）。
 */
public interface ChunkSessionStore {

    void create(ChunkSession session, Duration ttl);

    ChunkSession get(String uploadId);

    void markChunk(String uploadId, int index, Duration ttl);

    Set<Integer> received(String uploadId);

    void delete(String uploadId);
}
