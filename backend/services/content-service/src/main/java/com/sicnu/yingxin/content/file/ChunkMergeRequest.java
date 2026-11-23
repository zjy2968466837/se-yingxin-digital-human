package com.sicnu.yingxin.content.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 合并分片请求：可选传入整文件 SHA-256 做二次校验。
 */
@Data
@Schema(description = "合并分片请求")
public class ChunkMergeRequest {

    @Schema(description = "整文件 SHA-256（可选；传了就与合并结果比对，不一致返回 2708）",
            example = "3a7bd3e2360a3d29eea436fcfb7e44c735d117c42d1c1835420b6b9942dd4f1b")
    @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "fileHash 必须是 64 位十六进制 SHA-256")
    private String fileHash;
}
