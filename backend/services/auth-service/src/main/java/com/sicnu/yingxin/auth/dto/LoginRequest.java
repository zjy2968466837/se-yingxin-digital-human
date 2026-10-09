package com.sicnu.yingxin.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 学号 + 密码登录入参（F1.1）。
 */
@Schema(description = "学号密码登录请求")
public class LoginRequest {

    @Schema(description = "学号", example = "20260001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入学号")
    @Size(max = 32, message = "学号长度不能超过 32")
    private String studentNo;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 64, message = "密码长度需在 6–64 位之间")
    private String password;

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
