package com.sicnu.yingxin.content.file;

import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.file.storage.FileStorage;
import com.sicnu.yingxin.common.file.storage.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件能力：小文件批量上传、分片上传（后台重组）、下载、删除、批量临时地址。
 *
 * <p>设计要点：
 * <ul>
 *   <li>key 只由服务端按 SHA-256 生成，同内容同 key，天然去重（秒传）；</li>
 *   <li>分片先落临时目录，合并时流式拼接并边写边算 SHA-256，不把整文件读进内存；</li>
 *   <li>上传会话状态在 Redis（TTL 24h），超时自动过期，临时目录由合并/取消清理；</li>
 *   <li>只做基础设施能力，不含 FR-5/FR-6 的业务校验（材料项、报修单归属等留到业务阶段）。</li>
 * </ul>
 */
@Slf4j
@Service
public class FileStorageService {

    private static final int MAX_BATCH_URLS = 50;

    private final FileStorage storage;
    private final FileKeyGenerator keyGenerator;
    private final ChunkSessionStore sessionStore;
    private final FileUploadProperties properties;

    public FileStorageService(FileStorage storage,
                              FileKeyGenerator keyGenerator,
                              ChunkSessionStore sessionStore,
                              FileUploadProperties properties) {
        this.storage = storage;
        this.keyGenerator = keyGenerator;
        this.sessionStore = sessionStore;
        this.properties = properties;
    }

    // ------------------------------------------------------------------ 小文件批量

    public List<FileInfoVO> uploadBatch(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BizException(ErrorCode.PARAM_MISSING, "请选择要上传的文件");
        }
        if (files.length > properties.getMaxBatchCount()) {
            throw new BizException(ContentErrorCodes.TOO_MANY_FILES,
                    "一次最多上传 " + properties.getMaxBatchCount() + " 个文件");
        }
        List<FileInfoVO> result = new ArrayList<>(files.length);
        for (MultipartFile file : files) {
            result.add(uploadOne(file));
        }
        return result;
    }

    private FileInfoVO uploadOne(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ContentErrorCodes.FILE_TOO_LARGE, "文件为空");
        }
        if (file.getSize() > properties.getMaxFileSize()) {
            throw new BizException(ContentErrorCodes.FILE_TOO_LARGE,
                    "单个文件不能超过 " + (properties.getMaxFileSize() / 1024 / 1024) + " MB");
        }
        String fileName = keyGenerator.sanitizeFileName(file.getOriginalFilename());
        String extension = keyGenerator.extensionOf(fileName);
        String contentType = contentTypeOf(fileName);
        try {
            String hash = keyGenerator.sha256Hex(file.getInputStream());
            String key = keyGenerator.generate(hash, extension);
            if (!storage.exists(key)) {
                try (InputStream in = file.getInputStream()) {
                    storage.put(key, in, file.getSize(), contentType);
                }
            }
            return info(key, fileName, file.getSize(), contentType, hash);
        } catch (IOException e) {
            throw new BizException(ContentErrorCodes.STORAGE_UNAVAILABLE, "读取上传文件失败，请重试");
        } catch (FileStorageException e) {
            throw storageUnavailable(e);
        }
    }

    // ------------------------------------------------------------------ 分片上传

    public ChunkInitResponse initChunk(ChunkInitRequest request) {
        String fileName = keyGenerator.sanitizeFileName(request.getFileName());
        String extension = keyGenerator.extensionOf(fileName);
        if (request.getFileSize() > properties.getMaxFileSize()) {
            throw new BizException(ContentErrorCodes.FILE_TOO_LARGE,
                    "单个文件不能超过 " + (properties.getMaxFileSize() / 1024 / 1024) + " MB");
        }
        long chunkSize = request.getChunkSize() == null ? properties.getChunkSize() : request.getChunkSize();
        if (chunkSize <= 0 || chunkSize > properties.getMaxFileSize()) {
            throw new BizException(ErrorCode.PARAM_RANGE, "分片大小不合法");
        }
        long expectedChunks = (request.getFileSize() + chunkSize - 1) / chunkSize;
        if (request.getTotalChunks() != expectedChunks) {
            throw new BizException(ErrorCode.PARAM_RANGE,
                    "分片数与文件大小不匹配，期望 " + expectedChunks + " 片");
        }

        // 秒传：客户端给了整文件 hash，且对象已存在
        String declaredHash = request.getFileHash() == null ? null : request.getFileHash().toLowerCase(Locale.ROOT);
        if (declaredHash != null) {
            String key = keyGenerator.generate(declaredHash, extension);
            if (existsQuietly(key)) {
                ChunkInitResponse response = new ChunkInitResponse();
                response.setInstant(true);
                response.setTotalChunks(request.getTotalChunks());
                response.setChunkSize(chunkSize);
                response.setFile(info(key, fileName, request.getFileSize(),
                        contentTypeOf(request.getContentType(), fileName), declaredHash));
                return response;
            }
        }

        String uploadId = UUID.randomUUID().toString().replace("-", "");
        ChunkSession session = new ChunkSession();
        session.setUploadId(uploadId);
        session.setFileName(fileName);
        session.setFileSize(request.getFileSize());
        session.setChunkSize(chunkSize);
        session.setTotalChunks(request.getTotalChunks());
        session.setContentType(contentTypeOf(request.getContentType(), fileName));
        session.setDeclaredHash(declaredHash);
        session.setCreatedAt(LocalDateTime.now());
        sessionStore.create(session, ttl());

        try {
            Files.createDirectories(chunkDir(uploadId));
        } catch (IOException e) {
            throw new BizException(ContentErrorCodes.STORAGE_UNAVAILABLE, "创建分片临时目录失败");
        }

        ChunkInitResponse response = new ChunkInitResponse();
        response.setUploadId(uploadId);
        response.setChunkSize(chunkSize);
        response.setTotalChunks(request.getTotalChunks());
        response.setUploadedChunks(List.of());
        return response;
    }

    public int saveChunk(String uploadId, int index, MultipartFile chunk) {
        ChunkSession session = requireSession(uploadId);
        if (index < 0 || index >= session.getTotalChunks()) {
            throw new BizException(ErrorCode.PARAM_RANGE, "分片序号超出范围");
        }
        if (chunk == null || chunk.isEmpty()) {
            throw new BizException(ContentErrorCodes.FILE_TOO_LARGE, "分片内容为空");
        }
        if (chunk.getSize() > session.getChunkSize()) {
            throw new BizException(ContentErrorCodes.FILE_TOO_LARGE,
                    "分片不能超过 " + (session.getChunkSize() / 1024 / 1024) + " MB");
        }
        Path dir = chunkDir(uploadId);
        Path temp = dir.resolve(index + ".part.tmp");
        Path target = dir.resolve(index + ".part");
        try {
            Files.createDirectories(dir);
            chunk.transferTo(temp);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BizException(ContentErrorCodes.STORAGE_UNAVAILABLE, "保存分片失败，请重试");
        }
        sessionStore.markChunk(uploadId, index, ttl());
        return sessionStore.received(uploadId).size();
    }

    public FileInfoVO merge(String uploadId, String declaredHash) {
        ChunkSession session = requireSession(uploadId);
        Set<Integer> received = sessionStore.received(uploadId);
        for (int i = 0; i < session.getTotalChunks(); i++) {
            if (!received.contains(i)) {
                throw new BizException(ContentErrorCodes.CHUNK_MISSING, "分片 " + i + " 尚未上传，无法合并");
            }
        }

        Path dir = chunkDir(uploadId);
        Path merged = dir.resolve("merged.tmp");
        String hash;
        long size;
        try {
            hash = mergeChunks(dir, session.getTotalChunks(), merged);
            size = Files.size(merged);
        } catch (IOException e) {
            throw new BizException(ContentErrorCodes.STORAGE_UNAVAILABLE, "合并分片失败，请重试");
        }

        String expected = declaredHash != null
                ? declaredHash.toLowerCase(Locale.ROOT)
                : session.getDeclaredHash();
        if (expected != null && !expected.equalsIgnoreCase(hash)) {
            cleanupDir(uploadId);
            sessionStore.delete(uploadId);
            throw new BizException(ContentErrorCodes.FILE_HASH_MISMATCH, "文件校验失败：SHA-256 与声明值不一致");
        }
        if (size != session.getFileSize()) {
            cleanupDir(uploadId);
            sessionStore.delete(uploadId);
            throw new BizException(ContentErrorCodes.CHUNK_MISSING, "合并结果与声明大小不一致");
        }

        String extension = keyGenerator.extensionOf(session.getFileName());
        String key = keyGenerator.generate(hash, extension);
        if (!existsQuietly(key)) {
            try (InputStream in = Files.newInputStream(merged)) {
                storage.put(key, in, size, session.getContentType());
            } catch (IOException e) {
                throw new BizException(ContentErrorCodes.STORAGE_UNAVAILABLE, "读取合并文件失败");
            } catch (FileStorageException e) {
                throw storageUnavailable(e);
            }
        }

        cleanupDir(uploadId);
        sessionStore.delete(uploadId);
        return info(key, session.getFileName(), size, session.getContentType(), hash);
    }

    public void abort(String uploadId) {
        requireSession(uploadId);
        cleanupDir(uploadId);
        sessionStore.delete(uploadId);
    }

    // ------------------------------------------------------------------ 读取 / 删除

    public InputStream download(String key) {
        requireValidKey(key);
        if (!existsQuietly(key)) {
            throw new BizException(ContentErrorCodes.FILE_NOT_FOUND, "文件不存在或已被删除");
        }
        try {
            return storage.get(key);
        } catch (FileStorageException e) {
            throw storageUnavailable(e);
        }
    }

    public void delete(String key) {
        requireValidKey(key);
        if (!existsQuietly(key)) {
            throw new BizException(ContentErrorCodes.FILE_NOT_FOUND, "文件不存在或已被删除");
        }
        try {
            storage.delete(key);
        } catch (FileStorageException e) {
            throw storageUnavailable(e);
        }
    }

    public List<FileUrlVO> batchUrls(List<String> keys) {
        if (keys.size() > MAX_BATCH_URLS) {
            throw new BizException(ErrorCode.PARAM_RANGE, "一次最多获取 " + MAX_BATCH_URLS + " 个地址");
        }
        List<FileUrlVO> result = new ArrayList<>(keys.size());
        long expires = properties.getPresignTtlMinutes() * 60L;
        for (String key : keys) {
            requireValidKey(key);
            if (!existsQuietly(key)) {
                throw new BizException(ContentErrorCodes.FILE_NOT_FOUND, "文件不存在：" + key);
            }
            result.add(new FileUrlVO(key, urlOf(key), expires));
        }
        return result;
    }

    // ------------------------------------------------------------------ 内部工具

    private String mergeChunks(Path dir, int totalChunks, Path merged) throws IOException {
        try (OutputStream fileOut = Files.newOutputStream(merged);
             DigestOutputStream digestOut = new DigestOutputStream(
                     new BufferedOutputStream(fileOut), MessageDigest.getInstance("SHA-256"))) {
            for (int i = 0; i < totalChunks; i++) {
                try (InputStream in = Files.newInputStream(dir.resolve(i + ".part"))) {
                    in.transferTo(digestOut);
                }
            }
            digestOut.flush();
            return HexFormat.of().formatHex(digestOut.getMessageDigest().digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM 不支持 SHA-256", e);
        }
    }

    private ChunkSession requireSession(String uploadId) {
        ChunkSession session = sessionStore.get(uploadId);
        if (session == null) {
            throw new BizException(ContentErrorCodes.UPLOAD_SESSION_NOT_FOUND, "上传会话不存在或已过期，请重新初始化");
        }
        return session;
    }

    private void requireValidKey(String key) {
        if (!keyGenerator.isValidKey(key)) {
            throw new BizException(ErrorCode.PARAM_FORMAT, "非法的文件 key");
        }
    }

    private boolean existsQuietly(String key) {
        try {
            return storage.exists(key);
        } catch (FileStorageException e) {
            throw storageUnavailable(e);
        }
    }

    private FileInfoVO info(String key, String fileName, long size, String contentType, String hash) {
        FileInfoVO vo = new FileInfoVO();
        vo.setKey(key);
        vo.setFileName(fileName);
        vo.setSize(size);
        vo.setContentType(contentType);
        vo.setHash(hash);
        vo.setUrl(urlOf(key));
        return vo;
    }

    /** 预签名地址（MinIO/OSS）；本地实现没有直链，退回本服务的下载接口。 */
    private String urlOf(String key) {
        try {
            String presigned = storage.presignedGetUrl(key, Duration.ofMinutes(properties.getPresignTtlMinutes()));
            return presigned != null ? presigned : "/api/content/files/" + key;
        } catch (FileStorageException e) {
            throw storageUnavailable(e);
        }
    }

    private String contentTypeOf(String fileName) {
        return MediaTypeFactory.getMediaType(fileName)
                .map(MediaType::toString)
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    private String contentTypeOf(String declared, String fileName) {
        if (declared != null && !declared.isBlank()) {
            return declared;
        }
        return contentTypeOf(fileName);
    }

    private Duration ttl() {
        return Duration.ofHours(properties.getTtlHours());
    }

    private Path chunkDir(String uploadId) {
        return Path.of(properties.getDir(), uploadId);
    }

    private void cleanupDir(String uploadId) {
        Path dir = chunkDir(uploadId);
        if (!Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // 清理失败不影响主流程
                }
            });
        } catch (IOException ignored) {
            // 同上
        }
    }

    private BizException storageUnavailable(FileStorageException cause) {
        log.warn("文件存储操作失败: {}", cause.getMessage(), cause);
        return new BizException(ContentErrorCodes.STORAGE_UNAVAILABLE, "文件存储暂不可用，请稍后重试");
    }
}
