package com.sicnu.yingxin.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员登录请求（FR-9.1）：用户名 + 口令 + 图形验证码。
 *
 * <p>口令**只在本次请求中存在**：服务端校验后即丢弃，不写日志、不落库（开发规范 §5.5）。
 */
@Data
public class AdminLoginRequest {

    @NotBlank(message = "请输入用户名")
    @Size(max = 32, message = "用户名长度不能超过 32 个字符")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(max = 64, message = "密码长度不能超过 64 个字符")
    private String password;

    @NotBlank(message = "验证码已失效，请刷新后重试")
    private String captchaId;

    @NotBlank(message = "请输入验证码")
    @Size(min = 4, max = 4, message = "验证码为 4 位字符")
    private String captchaCode;
}
