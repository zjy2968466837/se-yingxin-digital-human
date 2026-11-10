package com.sicnu.yingxin.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员自助注册（课程演示用，生产必须由 {@code AUTH_REGISTER_ENABLED=false} 关闭）。
 *
 * <p>口令策略按开发规范 §5.5：8–64 位；确认口令只在服务端比对，不落库。
 */
@Data
public class AdminRegisterRequest {

    @NotBlank(message = "请输入用户名")
    @Pattern(regexp = "^[A-Za-z0-9_]{4,32}$", message = "用户名为 4–32 位字母、数字或下划线")
    private String username;

    @NotBlank(message = "请输入姓名")
    @Size(min = 2, max = 32, message = "姓名长度需为 2–32 个字符")
    private String realName;

    @NotBlank(message = "请输入密码")
    @Size(min = 8, max = 64, message = "密码长度需为 8–64 位")
    private String password;

    @NotBlank(message = "请再次输入密码")
    private String confirmPassword;

    @NotBlank(message = "验证码已失效，请刷新后重试")
    private String captchaId;

    @NotBlank(message = "请输入验证码")
    @Size(min = 4, max = 4, message = "验证码为 4 位字符")
    private String captchaCode;
}
