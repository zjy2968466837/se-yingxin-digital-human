package com.sicnu.yingxin.admin.controller;

import com.sicnu.yingxin.admin.client.AuthSessionClient;
import com.sicnu.yingxin.admin.config.AdminAuthProperties;
import com.sicnu.yingxin.admin.dto.AdminLoginRequest;
import com.sicnu.yingxin.admin.dto.AdminRegisterRequest;
import com.sicnu.yingxin.admin.entity.AdminUser;
import com.sicnu.yingxin.admin.mapper.AdminUserMapper;
import com.sicnu.yingxin.admin.service.AdminLoginService;
import com.sicnu.yingxin.admin.service.AdminRegisterService;
import com.sicnu.yingxin.admin.service.CaptchaService;
import com.sicnu.yingxin.admin.vo.AdminAuthResult;
import com.sicnu.yingxin.admin.vo.AdminLoginVO;
import com.sicnu.yingxin.admin.vo.AdminMeVO;
import com.sicnu.yingxin.admin.vo.CaptchaVO;
import com.sicnu.yingxin.common.core.constant.HeaderNames;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 管理后台认证（FR-9.1）：Session + Cookie 方案。
 *
 * <p>会话 id 只写进 **HttpOnly Cookie**（JS 读不到），响应体**不返回**令牌或会话 id；
 * 后续请求由浏览器自动携带 Cookie，网关校验 Redis 会话后注入 {@code X-User-*}。
 *
 * <p>白名单只放行 captcha / login / register / logout（开发规范 §4.2）；
 * {@code /me} 与其余 {@code /api/admin/**} 都要求有效会话，并且角色必须是 ADMIN（网关授权）。
 */
@Tag(name = "管理后台认证", description = "FR-9.1：验证码 + 账号密码登录/注册，HttpOnly Cookie 会话")
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final CaptchaService captchaService;
    private final AdminLoginService loginService;
    private final AdminRegisterService registerService;
    private final AuthSessionClient sessionClient;
    private final AdminUserMapper adminUserMapper;
    private final AdminAuthProperties properties;

    public AdminAuthController(CaptchaService captchaService, AdminLoginService loginService,
                               AdminRegisterService registerService, AuthSessionClient sessionClient,
                               AdminUserMapper adminUserMapper, AdminAuthProperties properties) {
        this.captchaService = captchaService;
        this.loginService = loginService;
        this.registerService = registerService;
        this.sessionClient = sessionClient;
        this.adminUserMapper = adminUserMapper;
        this.properties = properties;
    }

    @Operation(summary = "获取图形验证码", description = "返回 base64 data URL 与 captchaId；5 分钟有效，校验一次即作废")
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.ok(captchaService.generate());
    }

    @Operation(summary = "管理员登录", description = "验证码 + 用户名 + 口令；成功后 Set-Cookie（HttpOnly），响应体不含令牌")
    @PostMapping("/login")
    public Result<AdminLoginVO> login(
            @Parameter(description = "登录凭证", required = true) @Valid @RequestBody AdminLoginRequest request,
            HttpServletResponse response) {
        AdminAuthResult result = loginService.login(request);
        writeSessionCookie(response, result.sessionId(), result.user().expiresIn());
        return Result.ok(result.user());
    }

    @Operation(summary = "管理员注册", description = "课程演示用自助注册；生产由 AUTH_REGISTER_ENABLED=false 关闭。注册即登录")
    @PostMapping("/register")
    public Result<AdminLoginVO> register(
            @Parameter(description = "注册信息", required = true) @Valid @RequestBody AdminRegisterRequest request,
            HttpServletResponse response) {
        AdminAuthResult result = registerService.register(request);
        writeSessionCookie(response, result.sessionId(), result.user().expiresIn());
        return Result.ok(result.user());
    }

    @Operation(summary = "退出登录", description = "删除 Redis 会话并清除 Cookie；旧 Cookie 立即失效")
    @PostMapping("/logout")
    public Result<Void> logout(
            @CookieValue(value = HeaderNames.COOKIE_SESSION, required = false) String sessionId,
            HttpServletResponse response) {
        sessionClient.delete(sessionId);
        clearSessionCookie(response);
        return Result.ok();
    }

    @Operation(summary = "当前登录人", description = "路由守卫用：会话无效时网关直接 401；返回管理员基本信息")
    @GetMapping("/me")
    public Result<AdminMeVO> me(
            @RequestHeader(value = HeaderNames.USER_ID, required = false) String userIdHeader,
            @RequestHeader(value = HeaderNames.USER_ROLE, required = false) String role) {
        Long userId = parseUserId(userIdHeader);
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        AdminUser user = adminUserMapper.selectById(userId);
        if (user == null || user.getDeleted() == null || user.getDeleted() != 0L) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }
        return Result.ok(new AdminMeVO(user.getId(), user.getUsername(), user.getRealName(), role));
    }

    /** 会话 Cookie：HttpOnly（防 XSS 读取）+ SameSite（防 CSRF）+ Path=/ + Max-Age 与服务端 TTL 一致。 */
    private void writeSessionCookie(HttpServletResponse response, String sessionId, long maxAgeSeconds) {
        ResponseCookie cookie = baseCookie(sessionId)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearSessionCookie(HttpServletResponse response) {
        ResponseCookie cookie = baseCookie("").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(properties.getCookieName(), value)
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite(properties.getCookieSameSite())
                .path("/");
    }

    private static Long parseUserId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
