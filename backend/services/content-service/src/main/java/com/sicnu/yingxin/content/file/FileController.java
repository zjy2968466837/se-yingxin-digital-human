package com.sicnu.yingxin.content.file;

import com.sicnu.yingxin.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 文件能力接口。路径挂在 content-service 下，复用网关既有 {@code /api/content/**} 路由，不需要新增服务与端口。
 *
 * <p>下载接口返回二进制流，**不套统一响应体**（二进制没有 code/message/data 的位置），
 * 其余接口仍返回 {@code Result}。
 */
@Tag(name = "文件存储", description = "小文件批量上传 / 分片上传与后台重组 / 下载 / 删除 / 批量临时地址；后端由 yx.file.provider 选择 local、minio 或 oss")
@RestController
@RequestMapping("/api/content/files")
public class FileController {

    private final FileStorageService service;

    public FileController(FileStorageService service) {
        this.service = service;
    }

    @Operation(summary = "批量上传小文件",
            description = "multipart 一次最多 10 个文件；单个 ≤10 MB；按 SHA-256 去重，同内容直接复用已有对象")
    @PostMapping
    public Result<List<FileInfoVO>> upload(
            @Parameter(description = "文件列表（form-data 字段名 files）")
            @RequestParam("files") MultipartFile[] files) {
        return Result.ok(service.uploadBatch(files));
    }

    @Operation(summary = "初始化分片上传",
            description = "返回 uploadId 与服务端分片大小；若客户端带了整文件 SHA-256 且对象已存在，instant=true 直接秒传")
    @PostMapping("/chunks/init")
    public Result<ChunkInitResponse> initChunk(@Valid @RequestBody ChunkInitRequest request) {
        return Result.ok(service.initChunk(request));
    }

    @Operation(summary = "上传单个分片", description = "分片序号从 0 开始；返回已收到的分片数，便于前端显示进度/续传")
    @PutMapping("/chunks/{uploadId}/{index}")
    public Result<Map<String, Object>> uploadChunk(
            @Parameter(description = "上传会话 id") @PathVariable String uploadId,
            @Parameter(description = "分片序号，从 0 开始") @PathVariable int index,
            @Parameter(description = "分片内容（form-data 字段名 chunk）") @RequestPart("chunk") MultipartFile chunk) {
        int received = service.saveChunk(uploadId, index, chunk);
        return Result.ok(Map.of("received", received, "index", index));
    }

    @Operation(summary = "合并分片",
            description = "校验分片齐全后流式合并、边合并边算 SHA-256，再写入对象存储；可传 fileHash 做二次校验")
    @PostMapping("/chunks/{uploadId}/merge")
    public Result<FileInfoVO> merge(
            @Parameter(description = "上传会话 id") @PathVariable String uploadId,
            @RequestBody(required = false) ChunkMergeRequest request) {
        return Result.ok(service.merge(uploadId, request == null ? null : request.getFileHash()));
    }

    @Operation(summary = "取消分片上传", description = "删除临时分片与 Redis 会话")
    @DeleteMapping("/chunks/{uploadId}")
    public Result<Void> abort(@Parameter(description = "上传会话 id") @PathVariable String uploadId) {
        service.abort(uploadId);
        return Result.ok();
    }

    @Operation(summary = "下载文件",
            description = "返回二进制流（不是统一 Result 包装）；本地实现走本接口，MinIO/OSS 也可直接用预签名地址")
    @GetMapping("/{*key}")
    public ResponseEntity<InputStreamResource> download(
            @Parameter(description = "对象 key，含斜杠", example = "/2026/10/27/3a/3a7b...f1b.pdf")
            @PathVariable String key,
            @Parameter(description = "下载时展示的文件名，可选") @RequestParam(required = false) String name) {
        String normalized = key.startsWith("/") ? key.substring(1) : key;
        String fileName = (name == null || name.isBlank()) ? normalized.substring(normalized.lastIndexOf('/') + 1) : name;
        InputStream input = service.download(normalized);
        MediaType mediaType = MediaTypeFactory.getMediaType(fileName)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(input));
    }

    @Operation(summary = "删除文件", description = "按服务端生成的 key 删除对象；不暴露物理路径")
    @DeleteMapping
    public Result<Void> delete(
            @Parameter(description = "对象 key", required = true) @RequestParam String key) {
        service.delete(key);
        return Result.ok();
    }

    @Operation(summary = "批量获取临时地址",
            description = "一次最多 50 个 key；MinIO/OSS 返回预签名地址，本地实现返回下载接口路径")
    @PostMapping("/batch-urls")
    public Result<List<FileUrlVO>> batchUrls(@Valid @RequestBody BatchUrlRequest request) {
        return Result.ok(service.batchUrls(request.getKeys()));
    }
}
