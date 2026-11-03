package com.sicnu.yingxin.common.file.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.sicnu.yingxin.common.file.config.FileStorageProperties;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OSS 适配器单测：用 Mockito 模拟 OSS 客户端，不依赖真实 AK/SK 与网络。
 * 真机联调作为可选步骤（有云端凭证时把 FILE_PROVIDER 设为 oss）。
 */
class OssFileStorageTest {

    @Test
    void delegatesPutGetExistsDeleteAndPresign() throws Exception {
        OSS client = mock(OSS.class);
        OSSObject object = mock(OSSObject.class);
        ByteArrayInputStream content = new ByteArrayInputStream(new byte[]{1, 2, 3});
        URL url = new URL("https://bucket.oss-cn-chengdu.aliyuncs.com/a.txt?Signature=x");
        when(client.getObject("bucket", "a.txt")).thenReturn(object);
        when(object.getObjectContent()).thenReturn(content);
        when(client.doesObjectExist("bucket", "a.txt")).thenReturn(true);
        when(client.generatePresignedUrl(eq("bucket"), eq("a.txt"), any())).thenReturn(url);

        OssFileStorage storage = new OssFileStorage(client, "bucket");
        storage.put("a.txt", new ByteArrayInputStream(new byte[]{1}), 1, "text/plain");

        verify(client).putObject(eq("bucket"), eq("a.txt"),
                any(java.io.InputStream.class), any(ObjectMetadata.class));
        assertThat(storage.get("a.txt")).isSameAs(content);
        assertThat(storage.exists("a.txt")).isTrue();
        assertThat(storage.presignedGetUrl("a.txt", Duration.ofMinutes(10))).isEqualTo(url.toString());

        storage.delete("a.txt");
        verify(client).deleteObject("bucket", "a.txt");
        assertThat(storage.provider()).isEqualTo("oss");
    }
}
