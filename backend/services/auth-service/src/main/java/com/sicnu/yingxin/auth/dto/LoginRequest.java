package com.sicnu.yingxin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 账号密码登录请求。
 */
@Data
public class LoginRequest {

    /** 账号：学号或后台用户名 */
    @NotBlank(message = "账号不能为空")
    private String username;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 登录类型：1 学生 / 2 后台管理员 */
    private Integer loginType = 1;
}
