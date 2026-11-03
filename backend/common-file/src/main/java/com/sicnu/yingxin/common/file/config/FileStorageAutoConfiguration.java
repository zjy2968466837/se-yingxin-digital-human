package com.sicnu.yingxin.common.file.config;

import com.sicnu.yingxin.common.file.storage.FileStorage;
import com.sicnu.yingxin.common.file.storage.LocalFileStorage;
import com.sicnu.yingxin.common.file.storage.MinioFileStorage;
import com.sicnu.yingxin.common.file.storage.OssFileStorage;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * common-file 自动装配：业务服务引入依赖后，按 {@code yx.file.provider} 得到一个 {@link FileStorage} Bean。
 *
 * <p>三个 @Bean 的条件互斥，任一时刻只会装配一个实现；{@code local} 是默认值，保证离线/测试可用。
 */
@AutoConfiguration
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileStorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(FileStorage.class)
    @ConditionalOnProperty(prefix = "yx.file", name = "provider", havingValue = "minio")
    public FileStorage minioFileStorage(FileStorageProperties properties) {
        return new MinioFileStorage(properties.getMinio());
    }

    @Bean
    @ConditionalOnMissingBean(FileStorage.class)
    @ConditionalOnProperty(prefix = "yx.file", name = "provider", havingValue = "oss")
    public FileStorage ossFileStorage(FileStorageProperties properties) {
        return new OssFileStorage(properties.getOss());
    }

    @Bean
    @ConditionalOnMissingBean(FileStorage.class)
    @ConditionalOnProperty(prefix = "yx.file", name = "provider", havingValue = "local", matchIfMissing = true)
    public FileStorage localFileStorage(FileStorageProperties properties) {
        return new LocalFileStorage(properties.getLocal());
    }
}
