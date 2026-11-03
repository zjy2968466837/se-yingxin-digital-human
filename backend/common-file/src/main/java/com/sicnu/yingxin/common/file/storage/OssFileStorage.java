package com.sicnu.yingxin.common.file.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.sicnu.yingxin.common.file.config.FileStorageProperties;
import org.springframework.beans.factory.DisposableBean;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * 阿里云 OSS 实现（云端正式环境）。
 *
 * <p>AK/SK 只从环境变量注入（{@code OSS_ACCESS_KEY_ID} / {@code OSS_ACCESS_KEY_SECRET}），
 * 不写进仓库；本地无凭证时把 {@code FILE_PROVIDER} 设为 minio 或 local 即可，不会实例化本类。
 */
public class OssFileStorage implements FileStorage, DisposableBean {

    private final OSS client;
    private final String bucket;

    public OssFileStorage(FileStorageProperties.Oss properties) {
        this(new OSSClientBuilder().build(
                properties.getEndpoint(), properties.getAccessKeyId(), properties.getAccessKeySecret()), properties.getBucket());
    }

    /** 供单测注入 mock 客户端，避免依赖真实 AK/SK 与网络。 */
    OssFileStorage(OSS client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public void put(String key, InputStream input, long size, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        if (size >= 0) {
            metadata.setContentLength(size);
        }
        metadata.setContentType(contentType);
        try {
            client.putObject(bucket, key, input, metadata);
        } catch (RuntimeException e) {
            throw new FileStorageException("OSS 写入失败: " + key, e);
        }
    }

    @Override
    public InputStream get(String key) {
        try {
            return client.getObject(bucket, key).getObjectContent();
        } catch (RuntimeException e) {
            throw new FileStorageException("OSS 读取失败: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return client.doesObjectExist(bucket, key);
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.deleteObject(bucket, key);
        } catch (RuntimeException e) {
            throw new FileStorageException("OSS 删除失败: " + key, e);
        }
    }

    @Override
    public String presignedGetUrl(String key, Duration ttl) {
        try {
            Date expiration = Date.from(Instant.now().plus(ttl));
            URL url = client.generatePresignedUrl(bucket, key, expiration);
            return url == null ? null : url.toString();
        } catch (RuntimeException e) {
            throw new FileStorageException("OSS 生成临时地址失败: " + key, e);
        }
    }

    @Override
    public String provider() {
        return "oss";
    }

    @Override
    public void destroy() {
        client.shutdown();
    }
}
