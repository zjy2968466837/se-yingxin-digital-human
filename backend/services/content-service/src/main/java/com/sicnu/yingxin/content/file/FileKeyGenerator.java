package com.sicnu.yingxin.content.file;

import com.sicnu.yingxin.common.core.error.BizException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 对象 key 生成与校验：**key 只由服务端生成**，客户端文件名只用于取扩展名。
 *
 * <p>key 形如 {@code 2026/10/27/3a/3a7b...f1b.pdf}，前段是日期便于归档与按前缀清理，
 * 文件名是 SHA-256，天然支持去重（同内容同 key）。
 */
@Component
public class FileKeyGenerator {

    private static final Pattern KEY_PATTERN =
            Pattern.compile("^\\d{4}/\\d{2}/\\d{2}/[0-9a-f]{2}/[0-9a-f]{64}(\\.[a-z0-9]{1,10})?$");

    private final FileUploadProperties properties;

    public FileKeyGenerator(FileUploadProperties properties) {
        this.properties = properties;
    }

    /** 去掉客户端可能带的路径，只留文件名。 */
    public String sanitizeFileName(String original) {
        if (original == null || original.isBlank()) {
            return "file";
        }
        String name = original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.trim();
        return name.isBlank() ? "file" : name;
    }

    /** 扩展名必须存在且在白名单内；否则抛 2701。 */
    public String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new BizException(ContentErrorCodes.FILE_TYPE_NOT_ALLOWED, "文件缺少扩展名，无法判断类型");
        }
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!properties.getAllowedExtensions().contains(ext)) {
            throw new BizException(ContentErrorCodes.FILE_TYPE_NOT_ALLOWED, "不支持的文件类型：." + ext);
        }
        return ext;
    }

    public String generate(String sha256, String extension) {
        String hex = sha256.toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now();
        return "%04d/%02d/%02d/%s/%s.%s".formatted(
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(),
                hex.substring(0, 2), hex, extension);
    }

    /** 下载/删除前校验 key 形态，防止用任意 key 操作对象存储。 */
    public boolean isValidKey(String key) {
        return key != null && KEY_PATTERN.matcher(key).matches();
    }

    public String sha256Hex(InputStream input) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM 不支持 SHA-256", e);
        }
    }

    public String sha256Hex(byte[] content) {
        try {
            return sha256Hex(new java.io.ByteArrayInputStream(content));
        } catch (IOException e) {
            throw new IllegalStateException("计算 SHA-256 失败", e);
        }
    }
}
