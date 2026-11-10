package com.sicnu.yingxin.admin.controller;

import com.sicnu.yingxin.admin.dto.AdminLoginRequest;
import com.sicnu.yingxin.admin.service.AdminLoginService;
import com.sicnu.yingxin.admin.service.CaptchaService;
import com.sicnu.yingxin.admin.vo.AdminLoginVO;
import com.sicnu.yingxin.admin.vo.CaptchaVO;
import com.sicnu.yingxin.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台登录（FR-9.1）。
 *
 * <p>这两个路径由网关白名单放行（开发规范 §4.2）；登录成功后签发的令牌与
 * 学生端同源（HS256 + Redis 会话），网关统一校验。
 */
@Tag(name = "管理后台登录", description = "FR-9.1：图形验证码 + 账号密码登录")
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final CaptchaService captchaService;
    private final AdminLoginService loginService;

    public AdminAuthController(CaptchaService captchaService, AdminLoginService loginService) {
        this.captchaService = captchaService;
        this.loginService = loginService;
    }

    @Operation(summary = "获取图形验证码", description = "返回 base64 data URL 与 captchaId；5 分钟有效，校验一次即作废")
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.ok(captchaService.generate());
    }

    @Operation(summary = "管理员登录", description = "验证码 + 用户名 + 口令；口令按 BCrypt 比对，错误提示不区分账号是否存在")
    @PostMapping("/login")
    public Result<AdminLoginVO> login(
            @Parameter(description = "登录凭证", required = true) @Valid @RequestBody AdminLoginRequest request) {
        return Result.ok(loginService.login(request));
    }
}
