package com.sicnu.yingxin.auth.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sicnu.yingxin.auth.dto.LoginRequest;
import com.sicnu.yingxin.auth.dto.LoginResult;
import com.sicnu.yingxin.auth.entity.AuthUser;
import com.sicnu.yingxin.auth.mapper.AuthUserMapper;
import com.sicnu.yingxin.auth.service.JwtService;
import com.sicnu.yingxin.auth.service.SessionService;
import com.sicnu.yingxin.common.core.constant.HeaderNames;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.core.result.Result;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 认证与会话（F1.1 学号密码登录、F1.8 登出即失效）。
 *
 * <p>只有本服务签发令牌（开发规范 §4.1）；网关负责校验，其他服务只读网关注入的用户头。
 * 本服务是令牌的**签发方**，因此登出时允许读取 Authorization 并解析自己的令牌以删除会话。
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证与会话", description = "学号密码登录、登出（令牌签发方）")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /** 学生角色；后台账号（admin_user）走 admin-service 的 F9.1 登录，不在本节范围 */
    private static final String ROLE_STUDENT = "STUDENT";

    private final AuthUserMapper authUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessionService sessionService;

    public AuthController(AuthUserMapper authUserMapper, PasswordEncoder passwordEncoder,
                          JwtService jwtService, SessionService sessionService) {
        this.authUserMapper = authUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sessionService = sessionService;
    }

    @PostMapping("/login")
    @Operation(summary = "学号密码登录", description = "校验账号密码并签发访问令牌（白名单接口，无需令牌）")
    public Result<LoginResult> login(@Valid @RequestBody LoginRequest request) {
        AuthUser user = authUserMapper.selectOne(Wrappers.<AuthUser>lambdaQuery()
                .eq(AuthUser::getStudentNo, request.getStudentNo())
                .eq(AuthUser::getDeleted, AuthUser.NOT_DELETED));

        // 账号不存在与密码错误返回同一个码：不泄露「该学号是否存在」
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        if (!user.isActive()) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        JwtService.IssuedToken issued = jwtService.issue(user.getId(), ROLE_STUDENT);
        sessionService.save(issued.jti(), user.getId(), ROLE_STUDENT, jwtService.getAccessTokenTtl());

        AuthUser touch = new AuthUser();
        touch.setId(user.getId());
        touch.setLastLoginTime(LocalDateTime.now());
        authUserMapper.updateById(touch);

        log.info("登录成功 userId={} role={}", user.getId(), ROLE_STUDENT);
        return Result.ok(new LoginResult(issued.token(), issued.expiresInSeconds(), user.getId(), ROLE_STUDENT));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出", description = "删除服务端会话，旧令牌立即失效（F1.8 验收准则）")
    public Result<Void> logout(@RequestHeader(value = HeaderNames.AUTHORIZATION, required = false) String authorization) {
        String token = stripBearer(authorization);
        if (token == null) {
            return Result.ok();   // 无令牌视为已登出，幂等
        }
        try {
            Claims claims = jwtService.parse(token).getPayload();
            sessionService.remove(claims.getId());
            log.info("登出成功 jti={}", claims.getId());
        } catch (JwtException | IllegalArgumentException e) {
            // 令牌本身已失效：登出目标已达成，幂等返回成功
            log.debug("登出时令牌无效，按已登出处理");
        }
        return Result.ok();
    }

    private static String stripBearer(String authorization) {
        if (authorization == null || !authorization.startsWith(HeaderNames.BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(HeaderNames.BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
