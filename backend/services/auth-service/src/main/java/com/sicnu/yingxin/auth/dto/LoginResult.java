package com.sicnu.yingxin.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录结果：令牌 + 有效期 + 身份摘要（不含姓名、学号等隐私字段）。
 */
@Schema(description = "登录结果")
public class LoginResult {

    @Schema(description = "访问令牌（前端放 Authorization: Bearer <token>）")
    private String token;

    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "有效期（秒）", example = "7200")
    private long expiresIn;

    @Schema(description = "用户 id")
    private long userId;

    @Schema(description = "角色", example = "STUDENT")
    private String role;

    public LoginResult() {
    }

    public LoginResult(String token, long expiresIn, long userId, String role) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
