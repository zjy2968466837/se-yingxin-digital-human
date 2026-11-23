package com.sicnu.yingxin.auth.controller;

import com.sicnu.yingxin.auth.config.AuthProperties;
import com.sicnu.yingxin.auth.dto.InternalSessionRequest;
import com.sicnu.yingxin.auth.service.SessionService;
import com.sicnu.yingxin.auth.vo.SessionCreateVO;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 内部会话接口（FR-9.1）：给 admin-service 这类"自己校验凭证、但不该自己写会话"的服务用。
 *
 * <p>Session + Cookie 方案下，本接口只创建工作不透明 sessionId（Redis 存 userId/role）；
 * 浏览器拿到的 HttpOnly Cookie 由调用方（admin-service）在响应上设置。
 *
 * <p>三重限制：
 * ① 网关**不放行** {@code /api/auth/internal/**}（不在白名单）；
 * ② 调用方必须带 {@code X-Internal-Secret}，与 {@code AUTH_INTERNAL_SECRET} 做常量时间比较；
 * ③ 只接受 {@code userId / role}，不含口令与隐私字段。
 */
@Tag(name = "内部会话", description = "服务间调用：仅 admin-service 等受信服务使用，不经网关")
@RestController
@RequestMapping("/api/auth/internal")
public class InternalSessionController {

    private static final String SECRET_HEADER = "X-Internal-Secret";
    private static final int MAX_ROLE_LENGTH = 32;

    private final AuthProperties properties;
    private final SessionService sessionService;

    public InternalSessionController(AuthProperties properties, SessionService sessionService) {
        this.properties = properties;
        this.sessionService = sessionService;
    }

    @Operation(summary = "创建会话", description = "校验内部凭证后写入 Redis 会话，返回不透明 sessionId 与有效期")
    @PostMapping("/session")
    public Result<SessionCreateVO> create(
            @RequestHeader(value = SECRET_HEADER, required = false) String secret,
            @RequestBody InternalSessionRequest request) {
        verifySecret(secret);
        if (request == null || request.userId() == null || request.userId() <= 0
                || !StringUtils.hasText(request.role())) {
            throw new BizException(ErrorCode.PARAM_FORMAT, "userId 与 role 不能为空");
        }
        String role = request.role().trim();
        if (role.length() > MAX_ROLE_LENGTH) {
            throw new BizException(ErrorCode.PARAM_LENGTH, "role 长度不能超过 " + MAX_ROLE_LENGTH);
        }

        String sessionId = sessionService.create(request.userId(), role, properties.getAccessTokenTtl());
        return Result.ok(new SessionCreateVO(sessionId, properties.getAccessTokenTtl().toSeconds()));
    }

    @Operation(summary = "删除会话", description = "登出用：删除 Redis 会话键，旧 Cookie 立即失效")
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> remove(
            @RequestHeader(value = SECRET_HEADER, required = false) String secret,
            @PathVariable String sessionId) {
        verifySecret(secret);
        sessionService.remove(sessionId);
        return Result.ok();
    }

    /** 常量时间比较，避免按字节提前返回带来的时序侧信道。 */
    private void verifySecret(String provided) {
        boolean matched = StringUtils.hasText(provided)
                && MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8),
                properties.getInternalSecret().getBytes(StandardCharsets.UTF_8));
        if (!matched) {
            throw new BizException(ErrorCode.NO_PERMISSION, "内部接口凭证无效");
        }
    }
}
