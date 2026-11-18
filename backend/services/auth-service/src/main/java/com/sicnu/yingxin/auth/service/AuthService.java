package com.sicnu.yingxin.auth.service;

import com.sicnu.yingxin.auth.dto.LoginRequest;
import com.sicnu.yingxin.auth.vo.LoginVO;
import com.sicnu.yingxin.auth.vo.MeVO;

/**
 * 认证服务（业务阶段）：F1.1 学号密码登录 / F1.8 登出即失效 / 当前用户信息。
 */
public interface AuthService {

    LoginVO login(LoginRequest request, String clientIp, String userAgent);

    /** 登出：删除 Redis 会话，令牌立即失效。 */
    void logout(String token);

    /** 当前登录用户信息（含组与权限点）。 */
    MeVO me(Long userId);
}
