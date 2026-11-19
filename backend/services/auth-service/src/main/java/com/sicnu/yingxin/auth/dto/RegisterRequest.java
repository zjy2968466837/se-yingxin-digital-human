package com.sicnu.yingxin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 注册请求：选择用户组注册，敏感组注册后进入待审批状态。 */
@Data
public class RegisterRequest {

    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 32, message = "账号长度需在 4-32 位之间")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在 6-32 位之间")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 32, message = "姓名过长")
    private String realName;

    /** 注册选择的用户组编码，不允许选择 admin 组 */
    @NotBlank(message = "用户组不能为空")
    @Pattern(regexp = "[a-z][a-z0-9_]{1,31}", message = "用户组编码格式非法")
    private String groupCode;
}
