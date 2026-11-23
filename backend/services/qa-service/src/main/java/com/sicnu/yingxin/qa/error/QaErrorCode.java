package com.sicnu.yingxin.qa.error;

/**
 * qa-service 专属错误码（规范 §3.3，2201–2299 段）。
 */
public final class QaErrorCode {

    private QaErrorCode() {
    }

    /** 会话不存在 */
    public static final int SESSION_NOT_FOUND = 2201;

    /** 问题内容为空 */
    public static final int QUESTION_EMPTY = 2202;
}
