package com.sicnu.yingxin.common.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件存储配置（统一前缀 {@code yx.file}）。
 *
 * <p>与项目既有约定一致：yml 里一律用 {@code ${ENV:本地默认值}}，本地与云端共用一份文件、代码零改动。
 * 三个实现互斥，由 {@code yx.file.provider} 选择：local / minio / oss。
 */
@Data
@ConfigurationProperties(prefix = "yx.file")
public class FileStorageProperties {

    /** local（默认，离线可用）/ minio（自建）/ oss（云端） */
    private String provider = "local";

    private Local local = new Local();
    private Minio minio = new Minio();
    private Oss oss = new Oss();

    @Data
    public static class Local {
        /** 本地存储根目录；只在 provider=local 时生效 */
        private String root = "./data/files";
    }

    @Data
    public static class Minio {
        private String endpoint = "http://localhost:9000";
        private String bucket = "yingxin";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin";
    }

    @Data
    public static class Oss {
        /** 例：oss-cn-chengdu.aliyuncs.com */
        private String endpoint = "";
        private String bucket = "";
        private String accessKeyId = "";
        private String accessKeySecret = "";
    }
}
