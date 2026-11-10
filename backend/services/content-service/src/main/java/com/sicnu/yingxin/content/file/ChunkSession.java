package com.sicnu.yingxin.content.file;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分片上传会话（存 Redis，TTL 默认 24 小时）。
 */
@Data
public class ChunkSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uploadId;
    private String fileName;
    private long fileSize;
    private long chunkSize;
    private int totalChunks;
    private String contentType;
    private String declaredHash;
    private LocalDateTime createdAt;
}
