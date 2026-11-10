package com.sicnu.yingxin.auth.controller;

import com.sicnu.yingxin.auth.config.AuthProperties;
import com.sicnu.yingxin.auth.dto.InternalTokenRequest;
import com.sicnu.yingxin.auth.service.JwtService;
import com.sicnu.yingxin.auth.service.SessionService;
import com.sicnu.yingxin.auth.vo.TokenVO;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 内部令牌签发接口（FR-9.1）：给 admin-service 这类"自己校验凭证、但不该自己签令牌"的服务用。
 *
 * <p>三重限制：
 * ① 网关**不放行** {@code /api/auth/internal/**}（不在白名单），外部即使带令牌也进不来业务侧；
 * ② 调用方必须带 {@code X-Internal-Secret}，与 {@code AUTH_INTERNAL_SECRET} 做常量时间比较；
 * ③ 只接受 {@code userId / role}，不含口令与隐私字段，签发后写入与登录一致的 Redis 会话。
 */
@Tag(name = "内部令牌签发", description = "服务间调用：仅 admin-service 等受信服务使用，不经网关")
@RestController
@RequestMapping("/api/auth/internal")
public class InternalTokenController {

    private static final String SECRET_HEADER = "X-Internal-Secret";
    private static final int MAX_ROLE_LENGTH = 32;

    private final AuthProperties properties;
    private final JwtService jwtService;
    private final SessionService sessionService;

    public InternalTokenController(AuthProperties properties, JwtService jwtService,
                                   SessionService sessionService) {
        this.properties = properties;
        this.jwtService = jwtService;
        this.sessionService = sessionService;
    }

    @Operation(summary = "签发访问令牌", description = "校验内部凭证后签发 JWT 并写 Redis 会话；TTL 与令牌一致")
    @PostMapping("/token")
    public Result<TokenVO> issue(
            @RequestHeader(value = SECRET_HEADER, required = false) String secret,
            @RequestBody InternalTokenRequest request) {
        if (!secretMatches(secret)) {
            throw new BizException(ErrorCode.NO_PERMISSION, "内部接口凭证无效");
        }
        if (request == null || request.userId() == null || request.userId() <= 0
                || !StringUtils.hasText(request.role())) {
            throw new BizException(ErrorCode.PARAM_FORMAT, "userId 与 role 不能为空");
        }
        String role = request.role().trim();
        if (role.length() > MAX_ROLE_LENGTH) {
            throw new BizException(ErrorCode.PARAM_LENGTH, "role 长度不能超过 " + MAX_ROLE_LENGTH);
        }

        JwtService.IssuedToken issued = jwtService.issue(request.userId(), role);
        sessionService.save(issued.jti(), request.userId(), role, jwtService.getAccessTokenTtl());
        return Result.ok(new TokenVO(issued.token(), issued.expiresInSeconds()));
    }

    /** 常量时间比较，避免按字节提前返回带来的时序侧信道。 */
    private boolean secretMatches(String provided) {
        if (!StringUtils.hasText(provided)) {
            return false;
        }
        return MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8),
                properties.getInternalSecret().getBytes(StandardCharsets.UTF_8));
    }
}
