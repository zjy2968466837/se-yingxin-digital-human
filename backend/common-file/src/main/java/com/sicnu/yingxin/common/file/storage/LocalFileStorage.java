package com.sicnu.yingxin.common.file.storage;

import com.sicnu.yingxin.common.file.config.FileStorageProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * 本地磁盘实现：零外部依赖，适合单元测试与离线开发。
 *
 * <p>只用于开发/测试，不适合多实例部署（各实例各写各的盘）。生产走 MinIO 或 OSS。
 */
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(FileStorageProperties.Local properties) {
        this.root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
    }

    @Override
    public void put(String key, InputStream input, long size, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("写入本地文件失败: " + key, e);
        }
    }

    @Override
    public InputStream get(String key) {
        try {
            return Files.newInputStream(resolve(key));
        } catch (IOException e) {
            throw new FileStorageException("读取本地文件失败: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.isRegularFile(resolve(key));
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new FileStorageException("删除本地文件失败: " + key, e);
        }
    }

    @Override
    public String presignedGetUrl(String key, Duration ttl) {
        return null;
    }

    @Override
    public String provider() {
        return "local";
    }

    /** 防目录穿越：解析后的路径必须仍在 root 下。 */
    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new FileStorageException("非法的对象 key: " + key);
        }
        return target;
    }
}
