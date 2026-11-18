package com.sicnu.yingxin.auth.controller;

import com.sicnu.yingxin.auth.dto.LoginRequest;
import com.sicnu.yingxin.auth.dto.RegisterRequest;
import com.sicnu.yingxin.auth.service.AuthService;
import com.sicnu.yingxin.auth.service.RbacService;
import com.sicnu.yingxin.auth.vo.LoginVO;
import com.sicnu.yingxin.auth.vo.MeVO;
import com.sicnu.yingxin.auth.vo.TraceVO;
import com.sicnu.yingxin.common.core.constant.HeaderNames;
import com.sicnu.yingxin.common.web.context.UserContextHolder;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 认证接口：登录、注册、登出、当前用户、鉴权链路追踪。
 * 登录/注册在网关白名单（login 在代码内置白名单，register 经 extra-whitelist 放行）。
 */
@Tag(name = "认证服务", description = "登录、注册、登出、当前用户")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RbacService rbacService;

    @Operation(summary = "账号密码登录", description = "学生用学号登录，管理员用后台用户名登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return Result.ok(authService.login(request,
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")));
    }

    @Operation(summary = "注册", description = "选择用户组注册，敏感组注册后待审批")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(rbacService.register(request, request.getAccount()));
    }

    @Operation(summary = "登出", description = "删除 Redis 会话，令牌立即失效")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = HeaderNames.AUTHORIZATION, required = false) String authorization) {
        authService.logout(authorization);
        return Result.ok();
    }

    @Operation(summary = "当前用户", description = "返回账号、姓名、角色、用户组与权限点")
    @GetMapping("/me")
    public Result<MeVO> me() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return Result.ok(authService.me(userId));
    }

    @Operation(summary = "鉴权链路追踪", description = "当前用户对全部受保护路由的权限判定表")
    @GetMapping("/authz/trace")
    public Result<List<TraceVO>> trace() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return Result.ok(rbacService.trace(userId));
    }
}
