package com.sicnu.yingxin.content.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件元信息。业务表里保存的 {@code {name,url,hash}} 就是这里的子集（数据建模 §8.2）。
 */
@Data
@Schema(description = "文件元信息")
public class FileInfoVO {

    @Schema(description = "对象 key（服务端生成，yyyy/MM/dd/<hash前2位>/<sha256>.<ext>）",
            example = "2026/10/27/3a/3a7bd3e2360a3d29eea436fcfb7e44c735d117c42d1c1835420b6b9942dd4f1b.pdf")
    private String key;

    @Schema(description = "原始文件名（仅用于展示，不参与路径拼接）", example = "录取通知书.pdf")
    private String fileName;

    @Schema(description = "文件大小（字节）", example = "1048576")
    private Long size;

    @Schema(description = "MIME 类型", example = "application/pdf")
    private String contentType;

    @Schema(description = "SHA-256 十六进制（去重与完整性校验）",
            example = "3a7bd3e2360a3d29eea436fcfb7e44c735d117c42d1c1835420b6b9942dd4f1b")
    private String hash;

    @Schema(description = "访问地址：MinIO/OSS 为预签名临时地址，本地实现为下载接口路径")
    private String url;
}
