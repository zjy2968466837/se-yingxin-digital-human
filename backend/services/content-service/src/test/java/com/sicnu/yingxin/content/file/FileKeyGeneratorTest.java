package com.sicnu.yingxin.content.file;

import com.sicnu.yingxin.common.core.error.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileKeyGeneratorTest {

    private FileKeyGenerator generator() {
        FileUploadProperties properties = new FileUploadProperties();
        properties.setAllowedExtensions(List.of("txt", "pdf", "jpg"));
        return new FileKeyGenerator(properties);
    }

    @Test
    void stripsClientPathAndKeepsBaseName() {
        assertThat(generator().sanitizeFileName("C:\\Users\\a\\录取通知书.pdf")).isEqualTo("录取通知书.pdf");
        assertThat(generator().sanitizeFileName("../../etc/passwd")).isEqualTo("passwd");
        assertThat(generator().sanitizeFileName(null)).isEqualTo("file");
    }

    @Test
    void rejectsUnknownOrMissingExtension() {
        assertThat(generator().extensionOf("a.PDF")).isEqualTo("pdf");

        assertThatThrownBy(() -> generator().extensionOf("a.exe"))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ContentErrorCodes.FILE_TYPE_NOT_ALLOWED);
        assertThatThrownBy(() -> generator().extensionOf("README"))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ContentErrorCodes.FILE_TYPE_NOT_ALLOWED);
    }

    @Test
    void generatesDateShardedHashKeyAndValidatesIt() {
        String hash = "3a7bd3e2360a3d29eea436fcfb7e44c735d117c42d1c1835420b6b9942dd4f1b";
        String key = generator().generate(hash, "pdf");

        assertThat(key).startsWith("20").contains("/3a/" + hash + ".pdf");
        assertThat(generator().isValidKey(key)).isTrue();
        assertThat(generator().isValidKey("../../etc/passwd")).isFalse();
        assertThat(generator().isValidKey("2026/10/27/3a/not-a-hash.pdf")).isFalse();
    }
}
