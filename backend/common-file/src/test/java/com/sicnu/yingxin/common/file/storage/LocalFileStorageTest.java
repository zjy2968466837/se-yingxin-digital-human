package com.sicnu.yingxin.common.file.storage;

import com.sicnu.yingxin.common.file.config.FileStorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    private LocalFileStorage storage() {
        FileStorageProperties.Local properties = new FileStorageProperties.Local();
        properties.setRoot(tempDir.toString());
        return new LocalFileStorage(properties);
    }

    @Test
    void putGetExistsDeleteRoundTrip() throws Exception {
        LocalFileStorage storage = storage();
        byte[] content = "hello yingxin".getBytes(StandardCharsets.UTF_8);

        storage.put("2026/10/27/ab/demo.txt", new ByteArrayInputStream(content), content.length, "text/plain");

        assertThat(storage.exists("2026/10/27/ab/demo.txt")).isTrue();
        try (var in = storage.get("2026/10/27/ab/demo.txt")) {
            assertThat(in.readAllBytes()).isEqualTo(content);
        }

        storage.delete("2026/10/27/ab/demo.txt");
        assertThat(storage.exists("2026/10/27/ab/demo.txt")).isFalse();
        assertThat(storage.provider()).isEqualTo("local");
    }

    @Test
    void rejectsPathTraversal() {
        LocalFileStorage storage = storage();

        assertThatThrownBy(() -> storage.put("../escape.txt",
                new ByteArrayInputStream(new byte[]{1}), 1, "text/plain"))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("非法的对象 key");
    }
}
