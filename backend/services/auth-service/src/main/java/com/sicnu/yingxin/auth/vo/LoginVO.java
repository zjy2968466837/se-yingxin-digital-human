package com.sicnu.yingxin.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    /** 访问令牌 */
    private String accessToken;

    /** 令牌类型 */
    private String tokenType;

    /** 有效期（秒） */
    private Long expiresIn;

    /** 用户 ID */
    private Long userId;

    /** 角色 */
    private String role;
}
