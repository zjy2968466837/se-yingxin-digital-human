package com.sicnu.yingxin.content.file;

/**
 * content-service 专属错误码（段位 2701–2799，《开发规范》§3.3 附录登记）。
 */
public final class ContentErrorCodes {

    public static final int FILE_TYPE_NOT_ALLOWED = 2701;
    public static final int FILE_TOO_LARGE = 2702;
    public static final int UPLOAD_SESSION_NOT_FOUND = 2703;
    public static final int CHUNK_MISSING = 2704;
    public static final int STORAGE_UNAVAILABLE = 2705;
    public static final int FILE_NOT_FOUND = 2706;
    public static final int TOO_MANY_FILES = 2707;
    public static final int FILE_HASH_MISMATCH = 2708;

    private ContentErrorCodes() {
    }
}
