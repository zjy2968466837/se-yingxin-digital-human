package com.sicnu.yingxin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 登录请求：学生用学号，管理员用后台用户名。 */
@Data
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 登录类型：1 学生 / 2 后台管理员 */
    private Integer loginType = 1;
}
