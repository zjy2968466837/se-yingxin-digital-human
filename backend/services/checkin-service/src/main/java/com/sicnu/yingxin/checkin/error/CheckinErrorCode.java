package com.sicnu.yingxin.checkin.error;

/**
 * checkin-service 专属错误码（规范 §3.3，2301–2399 段）。
 */
public final class CheckinErrorCode {

    private CheckinErrorCode() {
    }

    /** 报到步骤不存在 */
    public static final int STEP_NOT_FOUND = 2301;

    /** 步骤编码冲突或已停用 */
    public static final int STEP_CODE_CONFLICT = 2302;

    /** 报到凭证未就绪（仍有步骤未完成） */
    public static final int CERT_NOT_READY = 2304;
}
