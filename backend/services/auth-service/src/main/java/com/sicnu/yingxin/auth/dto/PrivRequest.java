package com.sicnu.yingxin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 权限矩阵勾选请求：为组授予或回收一个权限点。 */
@Data
public class PrivRequest {

    @NotBlank(message = "组编码不能为空")
    private String groupCode;

    /** 权限点编码，格式 module:priv */
    @NotBlank(message = "权限点不能为空")
    @Pattern(regexp = "[a-z][a-z0-9_]*:[a-z][a-z0-9_]*", message = "权限点格式应为 module:priv")
    private String priv;

    /** true 授予 / false 回收 */
    @NotNull(message = "授权结果不能为空")
    private Boolean grant;
}
