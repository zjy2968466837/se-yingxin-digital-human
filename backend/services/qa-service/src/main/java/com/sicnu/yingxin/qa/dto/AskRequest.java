package com.sicnu.yingxin.qa.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提问请求。
 */
@Data
public class AskRequest {

    /** 会话 ID */
    @NotNull(message = "会话 ID 不能为空")
    private Long sessionId;

    /** 用户问题 */
    private String question;
}
