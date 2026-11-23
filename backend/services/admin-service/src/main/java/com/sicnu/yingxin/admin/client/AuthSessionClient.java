package com.sicnu.yingxin.admin.client;

import com.sicnu.yingxin.admin.config.AdminAuthProperties;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.core.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 调用 auth-service 内部会话接口（FR-9.1）。
 *
 * <p>为什么不让 admin-service 自己写会话：会话存储口径统一在 auth-service（key 前缀、TTL、登出语义），
 * 否则两处各写一套会漂移。本调用**直连 8100，不经网关**，网关对
 * {@code /api/auth/internal/**} 不放行；接口本身再校验 {@code X-Internal-Secret}。
 *
 * <p>口令不进入本调用：admin-service 校验完 BCrypt 后只传 {@code userId / role}。
 */
@Component
public class AuthSessionClient {

    private static final Logger log = LoggerFactory.getLogger(AuthSessionClient.class);
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final RestClient restClient;
    private final AdminAuthProperties properties;

    public AuthSessionClient(RestClient.Builder builder, AdminAuthProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
    }

    /** 创建会话；失败抛 9005，登录/注册整体失败（不能下发无效 Cookie）。 */
    public SessionResponse create(long userId, String role) {
        try {
            Result<SessionResponse> result = restClient.post()
                    .uri(properties.getSessionUrl())
                    .header(INTERNAL_SECRET_HEADER, properties.getInternalSecret())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SessionRequest(userId, role))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Result<SessionResponse>>() {
                    });
            if (result == null || result.getCode() != 0 || result.getData() == null) {
                log.warn("auth-service 创建会话失败 userId={} code={}", userId, result == null ? null : result.getCode());
                throw new BizException(ErrorCode.DOWNSTREAM_UNAVAILABLE, "认证服务暂时不可用，请稍后重试");
            }
            return result.getData();
        } catch (RestClientException e) {
            log.error("调用 auth-service 失败 userId={} reason={}", userId, e.getMessage());
            throw new BizException(ErrorCode.DOWNSTREAM_UNAVAILABLE, "认证服务暂时不可用，请稍后重试");
        }
    }

    /** 删除会话（登出）：失败只记日志，不能让"清 Cookie"失败。 */
    public void delete(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        try {
            restClient.delete()
                    .uri(properties.getSessionUrl() + "/" + sessionId)
                    .header(INTERNAL_SECRET_HEADER, properties.getInternalSecret())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("删除会话失败，Cookie 仍会被清除 sessionId={} reason={}", sessionId, e.getMessage());
        }
    }

    public record SessionRequest(long userId, String role) {
    }

    public record SessionResponse(String sessionId, long expiresIn) {
    }
}
