package com.sicnu.yingxin.common.file.storage;

/**
 * 存储层异常：SDK 的网络错误、鉴权失败、对象不存在都收敛成这一种，
 * 由业务服务翻译成自己的错误码（content-service 用 2701–2799 段）。
 */
public class FileStorageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
