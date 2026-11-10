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
 * 调用 auth-service 内部接口签发令牌（FR-9.1）。
 *
 * <p>为什么不让 admin-service 自己签：令牌签发权统一在 auth-service（会话键也由它写），
 * 否则两处各签一套会漂移。本调用**直连 8100，不经网关**，网关对
 * {@code /api/auth/internal/**} 不放行；接口本身再校验 {@code X-Internal-Secret}。
 *
 * <p>口令不进入本调用：admin-service 校验完 BCrypt 后只传 {@code userId / role}。
 */
@Component
public class AuthTokenClient {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenClient.class);
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final RestClient restClient;
    private final AdminAuthProperties properties;

    public AuthTokenClient(RestClient.Builder builder, AdminAuthProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
    }

    public TokenResponse issue(long userId, String role) {
        try {
            Result<TokenResponse> result = restClient.post()
                    .uri(properties.getTokenIssueUrl())
                    .header(INTERNAL_SECRET_HEADER, properties.getInternalSecret())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TokenRequest(userId, role))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Result<TokenResponse>>() {
                    });
            if (result == null || result.getCode() != 0 || result.getData() == null) {
                log.warn("auth-service 签发令牌失败 userId={} code={}", userId, result == null ? null : result.getCode());
                throw new BizException(ErrorCode.DOWNSTREAM_UNAVAILABLE, "认证服务暂时不可用，请稍后重试");
            }
            return result.getData();
        } catch (RestClientException e) {
            // 内部凭证与地址都不进日志，只记 userId 与异常摘要
            log.error("调用 auth-service 失败 userId={} reason={}", userId, e.getMessage());
            throw new BizException(ErrorCode.DOWNSTREAM_UNAVAILABLE, "认证服务暂时不可用，请稍后重试");
        }
    }

    public record TokenRequest(long userId, String role) {
    }

    public record TokenResponse(String token, long expiresIn) {
    }
}
