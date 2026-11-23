package com.sicnu.yingxin.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 变更用户组请求（整组替换，事务内完成）。 */
@Data
public class UserGroupRequest {

    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    @NotEmpty(message = "用户组列表不能为空")
    private List<String> groupCodes;
}
