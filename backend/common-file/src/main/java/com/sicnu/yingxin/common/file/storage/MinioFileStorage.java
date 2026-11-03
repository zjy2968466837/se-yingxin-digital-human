package com.sicnu.yingxin.common.file.storage;

import com.sicnu.yingxin.common.file.config.FileStorageProperties;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MinIO 实现（开源文件服务器，S3 兼容）。
 *
 * <p>建桶是惰性的：应用启动时 MinIO 未就绪不应导致启动失败，第一次写入时再建桶。
 */
public class MinioFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(MinioFileStorage.class);

    private final MinioClient client;
    private final String bucket;
    private final AtomicBoolean bucketReady = new AtomicBoolean(false);

    public MinioFileStorage(FileStorageProperties.Minio properties) {
        this.bucket = properties.getBucket();
        this.client = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Override
    public void put(String key, InputStream input, long size, String contentType) {
        try {
            ensureBucket();
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(input, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new FileStorageException("MinIO 写入失败: " + key, e);
        }
    }

    @Override
    public InputStream get(String key) {
        try {
            return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            throw new FileStorageException("MinIO 读取失败: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            client.statObject(StatObjectArgs.builder().bucket(bucket).object(key).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            throw new FileStorageException("MinIO 删除失败: " + key, e);
        }
    }

    @Override
    public String presignedGetUrl(String key, Duration ttl) {
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(key)
                    .expiry((int) Math.max(1, Math.min(ttl.getSeconds(), 7 * 24 * 3600L)))
                    .build());
        } catch (Exception e) {
            throw new FileStorageException("MinIO 生成临时地址失败: " + key, e);
        }
    }

    @Override
    public String provider() {
        return "minio";
    }

    private void ensureBucket() throws Exception {
        if (bucketReady.get()) {
            return;
        }
        synchronized (bucketReady) {
            if (bucketReady.get()) {
                return;
            }
            // 直接建桶并忽略"已存在"：部分 S3 兼容实现（rclone、Ceph RGW）的 HEAD bucket
            // 对不存在的桶也返回 200，只用 bucketExists 判断会漏建。
            try {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("对象存储桶已创建: {}", bucket);
            } catch (io.minio.errors.ErrorResponseException e) {
                String code = e.errorResponse() == null ? "" : e.errorResponse().code();
                if (!"BucketAlreadyOwnedByYou".equals(code)
                        && !"BucketAlreadyExists".equals(code)
                        && !"BucketNotEmpty".equals(code)) {
                    throw e;
                }
            }
            bucketReady.set(true);
        }
    }
}
