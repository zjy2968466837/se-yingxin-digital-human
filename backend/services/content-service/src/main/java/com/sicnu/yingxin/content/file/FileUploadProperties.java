package com.sicnu.yingxin.content.file;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 上传能力配置（{@code yx.file.upload}）。存储后端的配置在 common-file 的 {@code yx.file.*} 下，两者不冲突。
 */
@Data
@Component
@ConfigurationProperties(prefix = "yx.file.upload")
public class FileUploadProperties {

    /** 分片临时目录 */
    private String dir = System.getProperty("user.home") + "/.yingxin/chunks";

    /** 默认分片大小：5 MB */
    private long chunkSize = 5L * 1024 * 1024;

    /** 单文件上限：100 MB（需求文档的单文件 10 MB 是业务限制，这里给基础设施上限） */
    private long maxFileSize = 100L * 1024 * 1024;

    /** 上传会话有效期（小时） */
    private int ttlHours = 24;

    /** 小文件批量上传单次最多文件数 */
    private int maxBatchCount = 10;

    /** 预签名下载地址有效期（分钟） */
    private int presignTtlMinutes = 30;

    /** 允许的扩展名（小写、不带点） */
    private List<String> allowedExtensions = List.of(
            "jpg", "jpeg", "png", "gif", "pdf", "doc", "docx",
            "xls", "xlsx", "ppt", "pptx", "zip", "txt");
}
