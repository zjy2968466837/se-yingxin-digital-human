package com.sicnu.yingxin.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 注册审批请求。 */
@Data
public class ApproveRequest {

    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    /** true 通过 / false 驳回 */
    @NotNull(message = "审批结果不能为空")
    private Boolean approve;
}
