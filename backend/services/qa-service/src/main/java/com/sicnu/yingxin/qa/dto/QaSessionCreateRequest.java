package com.sicnu.yingxin.qa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建会话请求。接口出入参一律用 DTO/VO，禁止直接暴露 Entity（开发规范 §2.1）。
 */
@Data
@Schema(description = "创建问答会话请求")
public class QaSessionCreateRequest {

    @Schema(description = "学生用户 id（雪花 ID）", example = "1000123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "userId 不能为空")
    @Min(value = 1, message = "userId 必须为正数")
    private Long userId;

    @Schema(description = "来源渠道：1 学生端网页 / 2 管理后台", example = "1", defaultValue = "1")
    @Min(value = 1, message = "channel 取值为 1–2")
    @Max(value = 2, message = "channel 取值为 1–2")
    private Integer channel = 1;
}
