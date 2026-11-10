package com.sicnu.yingxin.content.file;

import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.file.storage.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 文件能力单测：用内存存储 + 内存会话，不连 MinIO/Redis（开发规范 §6 测试分层）。
 */
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private InMemoryStorage storage;
    private InMemorySessionStore sessions;
    private FileStorageService service;

    @BeforeEach
    void setUp() {
        FileUploadProperties properties = new FileUploadProperties();
        properties.setDir(tempDir.toString());
        properties.setChunkSize(5);
        properties.setMaxFileSize(1024);
        properties.setMaxBatchCount(3);
        properties.setAllowedExtensions(List.of("txt", "pdf"));
        storage = new InMemoryStorage();
        sessions = new InMemorySessionStore();
        service = new FileStorageService(storage, new FileKeyGenerator(properties), sessions, properties);
    }

    @Test
    void uploadBatchStoresObjectAndDeduplicates() {
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("files", "a.txt", "text/plain", content);

        List<FileInfoVO> first = service.uploadBatch(new MockMultipartFile[]{file});
        List<FileInfoVO> second = service.uploadBatch(new MockMultipartFile[]{
                new MockMultipartFile("files", "b.txt", "text/plain", content)});

        assertThat(first).hasSize(1);
        assertThat(first.get(0).getKey()).endsWith(".txt");
        assertThat(first.get(0).getHash()).isEqualTo(sha256(content));
        assertThat(second.get(0).getKey()).isEqualTo(first.get(0).getKey());   // 同内容同 key
        assertThat(storage.putCount).isEqualTo(1);                          // 第二次命中已存在对象
        assertThat(storage.objects.get(first.get(0).getKey())).isEqualTo(content);
    }

    @Test
    void rejectsDisallowedExtension() {
        MockMultipartFile file = new MockMultipartFile("files", "virus.exe", "application/octet-stream", new byte[]{1});

        assertThatThrownBy(() -> service.uploadBatch(new MockMultipartFile[]{file}))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ContentErrorCodes.FILE_TYPE_NOT_ALLOWED);
    }

    @Test
    void chunkedUploadMergesAndCleansUp() throws IOException {
        byte[] content = "hello world!".getBytes(StandardCharsets.UTF_8);   // 12 字节 → 3 片（5/5/2）
        ChunkInitRequest request = new ChunkInitRequest();
        request.setFileName("report.txt");
        request.setFileSize((long) content.length);
        request.setTotalChunks(3);
        request.setChunkSize(5L);

        ChunkInitResponse init = service.initChunk(request);
        assertThat(init.getTotalChunks()).isEqualTo(3);
        assertThat(init.isInstant()).isFalse();

        service.saveChunk(init.getUploadId(), 0, part(content, 0, 5));
        service.saveChunk(init.getUploadId(), 1, part(content, 5, 10));
        service.saveChunk(init.getUploadId(), 2, part(content, 10, 12));

        FileInfoVO merged = service.merge(init.getUploadId(), sha256(content));

        assertThat(merged.getHash()).isEqualTo(sha256(content));
        assertThat(merged.getSize()).isEqualTo(content.length);
        assertThat(storage.objects.get(merged.getKey())).isEqualTo(content);
        assertThat(sessions.get(init.getUploadId())).isNull();
        assertThat(tempDir.resolve(init.getUploadId())).doesNotExist();
    }

    @Test
    void mergeRejectsMissingChunk() {
        ChunkInitRequest request = new ChunkInitRequest();
        request.setFileName("report.txt");
        request.setFileSize(12L);
        request.setTotalChunks(3);
        request.setChunkSize(5L);
        ChunkInitResponse init = service.initChunk(request);
        service.saveChunk(init.getUploadId(), 0,
                new MockMultipartFile("chunk", "0.part", "application/octet-stream", "hello".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> service.merge(init.getUploadId(), null))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ContentErrorCodes.CHUNK_MISSING);
    }

    @Test
    void mergeRejectsHashMismatch() {
        ChunkInitRequest request = new ChunkInitRequest();
        request.setFileName("report.txt");
        request.setFileSize(5L);
        request.setTotalChunks(1);
        request.setChunkSize(5L);
        ChunkInitResponse init = service.initChunk(request);
        service.saveChunk(init.getUploadId(), 0,
                new MockMultipartFile("chunk", "0.part", "application/octet-stream", "hello".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> service.merge(init.getUploadId(), "0".repeat(64)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ContentErrorCodes.FILE_HASH_MISMATCH);
    }

    @Test
    void initReturnsInstantWhenObjectAlreadyExists() {
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        String hash = sha256(content);
        String key = new FileKeyGenerator(properties()).generate(hash, "txt");
        storage.objects.put(key, content);

        ChunkInitRequest request = new ChunkInitRequest();
        request.setFileName("a.txt");
        request.setFileSize((long) content.length);
        request.setTotalChunks(1);
        request.setFileHash(hash);

        ChunkInitResponse response = service.initChunk(request);

        assertThat(response.isInstant()).isTrue();
        assertThat(response.getFile().getKey()).isEqualTo(key);
        assertThat(response.getUploadId()).isNull();
    }

    private FileUploadProperties properties() {
        FileUploadProperties properties = new FileUploadProperties();
        properties.setDir(tempDir.toString());
        properties.setChunkSize(5);
        properties.setAllowedExtensions(List.of("txt", "pdf"));
        return properties;
    }

    private MockMultipartFile part(byte[] content, int from, int to) {
        byte[] slice = java.util.Arrays.copyOfRange(content, from, to);
        return new MockMultipartFile("chunk", "part", "application/octet-stream", slice);
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static class InMemoryStorage implements FileStorage {
        final Map<String, byte[]> objects = new HashMap<>();
        int putCount;

        @Override
        public void put(String key, InputStream input, long size, String contentType) {
            try {
                objects.put(key, input.readAllBytes());
                putCount++;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public InputStream get(String key) {
            return new ByteArrayInputStream(objects.get(key));
        }

        @Override
        public boolean exists(String key) {
            return objects.containsKey(key);
        }

        @Override
        public void delete(String key) {
            objects.remove(key);
        }

        @Override
        public String presignedGetUrl(String key, Duration ttl) {
            return "https://example.test/" + key;
        }

        @Override
        public String provider() {
            return "memory";
        }
    }

    static class InMemorySessionStore implements ChunkSessionStore {
        final Map<String, ChunkSession> sessions = new HashMap<>();
        final Map<String, Set<Integer>> chunks = new HashMap<>();

        @Override
        public void create(ChunkSession session, Duration ttl) {
            sessions.put(session.getUploadId(), session);
            chunks.put(session.getUploadId(), new HashSet<>());
        }

        @Override
        public ChunkSession get(String uploadId) {
            return sessions.get(uploadId);
        }

        @Override
        public void markChunk(String uploadId, int index, Duration ttl) {
            chunks.computeIfAbsent(uploadId, k -> new HashSet<>()).add(index);
        }

        @Override
        public Set<Integer> received(String uploadId) {
            return new HashSet<>(chunks.getOrDefault(uploadId, Set.of()));
        }

        @Override
        public void delete(String uploadId) {
            sessions.remove(uploadId);
            chunks.remove(uploadId);
        }
    }
}
