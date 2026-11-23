package com.sicnu.yingxin.common.file.storage;

import java.io.InputStream;
import java.time.Duration;

/**
 * 文件存储抽象（与协议无关）：业务服务只管把流交给实现，不关心是本地盘、MinIO 还是 OSS。
 *
 * <p>实现方必须保证：{@code key} 由服务端生成（形如 {@code 2026/10/27/ab/<sha256>.pdf}），
 * 不接受客户端文件名；读取返回的流由调用方关闭。
 */
public interface FileStorage {

    /** 写入对象；{@code size < 0} 表示未知长度（仅部分实现支持）。 */
    void put(String key, InputStream input, long size, String contentType);

    /** 读取对象流；对象不存在时抛 {@link FileStorageException}。 */
    InputStream get(String key);

    boolean exists(String key);

    void delete(String key);

    /**
     * 生成临时下载地址（预签名）。本地实现不支持直链，返回 {@code null}，
     * 由业务服务走自己的下载接口。
     */
    String presignedGetUrl(String key, Duration ttl);

    /** 实现标识：local / minio / oss，用于日志与自检。 */
    String provider();
}
