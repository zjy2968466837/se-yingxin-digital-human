package com.sicnu.yingxin.content.file;

import com.sicnu.yingxin.common.core.constant.CacheKeys;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * 分片会话的 Redis 实现。
 *
 * <p>两个 key：{@code yx:content:upload:{uploadId}} 存会话（JSON），
 * {@code yx:content:upload:{uploadId}:chunks} 用 Set 存已收到分片序号，二者 TTL 一致。
 * Redis 不可用时直接返回 9003，不让"半套状态"继续合并。
 */
@Component
public class RedisChunkSessionStore implements ChunkSessionStore {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisChunkSessionStore(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void create(ChunkSession session, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(CacheKeys.contentUpload(session.getUploadId()), session, ttl);
            redisTemplate.delete(CacheKeys.contentUploadChunks(session.getUploadId()));
        } catch (RuntimeException e) {
            throw unavailable(e);
        }
    }

    @Override
    public ChunkSession get(String uploadId) {
        try {
            Object value = redisTemplate.opsForValue().get(CacheKeys.contentUpload(uploadId));
            return value instanceof ChunkSession session ? session : null;
        } catch (RuntimeException e) {
            throw unavailable(e);
        }
    }

    @Override
    public void markChunk(String uploadId, int index, Duration ttl) {
        try {
            String key = CacheKeys.contentUploadChunks(uploadId);
            redisTemplate.opsForSet().add(key, index);
            redisTemplate.expire(key, ttl);
        } catch (RuntimeException e) {
            throw unavailable(e);
        }
    }

    @Override
    public Set<Integer> received(String uploadId) {
        try {
            Set<Object> members = redisTemplate.opsForSet().members(CacheKeys.contentUploadChunks(uploadId));
            Set<Integer> indexes = new HashSet<>();
            if (members != null) {
                for (Object member : members) {
                    if (member instanceof Number number) {
                        indexes.add(number.intValue());
                    } else if (member != null) {
                        indexes.add(Integer.parseInt(member.toString()));
                    }
                }
            }
            return indexes;
        } catch (RuntimeException e) {
            throw unavailable(e);
        }
    }

    @Override
    public void delete(String uploadId) {
        try {
            redisTemplate.delete(CacheKeys.contentUpload(uploadId));
            redisTemplate.delete(CacheKeys.contentUploadChunks(uploadId));
        } catch (RuntimeException e) {
            throw unavailable(e);
        }
    }

    private BizException unavailable(RuntimeException e) {
        return new BizException(ErrorCode.CACHE_ERROR, "上传会话存储不可用，请稍后重试");
    }
}
