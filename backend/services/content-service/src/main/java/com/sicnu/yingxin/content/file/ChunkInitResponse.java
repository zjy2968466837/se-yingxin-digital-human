package com.sicnu.yingxin.content.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分片上传初始化结果：{@code instant=true} 表示命中秒传，无需再传分片。
 */
@Data
@Schema(description = "分片上传初始化结果")
public class ChunkInitResponse {

    @Schema(description = "上传会话 id；秒传时为空", example = "8f14e45fceea167a5a36dedd4bea2543")
    private String uploadId;

    @Schema(description = "服务端分片大小（字节）", example = "5242880")
    private Long chunkSize;

    @Schema(description = "分片总数", example = "4")
    private Integer totalChunks;

    @Schema(description = "已收到的分片序号（用于断点续传）")
    private List<Integer> uploadedChunks = new ArrayList<>();

    @Schema(description = "是否命中秒传（服务端已有相同 SHA-256 的对象）")
    private boolean instant;

    @Schema(description = "秒传命中的文件信息；非秒传为 null")
    private FileInfoVO file;
}
