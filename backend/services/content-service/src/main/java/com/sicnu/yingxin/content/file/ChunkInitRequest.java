package com.sicnu.yingxin.content.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 分片上传初始化请求。
 */
@Data
@Schema(description = "分片上传初始化请求")
public class ChunkInitRequest {

    @Schema(description = "原始文件名（含扩展名，用于类型白名单校验）", example = "报到材料.pdf", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "fileName 不能为空")
    private String fileName;

    @Schema(description = "文件总大小（字节）", example = "20971520", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 1, message = "fileSize 必须大于 0")
    private Long fileSize;

    @Schema(description = "分片总数", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 1, message = "totalChunks 必须大于 0")
    private Integer totalChunks;

    @Schema(description = "分片大小（字节），不传则用服务端默认 5 MB", example = "5242880")
    private Long chunkSize;

    @Schema(description = "MIME 类型", example = "application/pdf")
    private String contentType;

    @Schema(description = "整文件 SHA-256（可选）：传了可命中秒传，合并后也会校验",
            example = "3a7bd3e2360a3d29eea436fcfb7e44c735d117c42d1c1835420b6b9942dd4f1b")
    @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "fileHash 必须是 64 位十六进制 SHA-256")
    private String fileHash;
}
