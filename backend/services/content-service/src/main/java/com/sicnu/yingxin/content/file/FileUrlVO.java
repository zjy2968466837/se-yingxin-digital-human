package com.sicnu.yingxin.content.file;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 批量临时地址。
 */
@Schema(description = "临时下载地址")
public record FileUrlVO(
        @Schema(description = "对象 key") String key,
        @Schema(description = "临时访问地址") String url,
        @Schema(description = "有效期（秒）") long expiresInSeconds) {
}
