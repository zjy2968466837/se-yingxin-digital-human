package com.sicnu.yingxin.auth.service;

import com.sicnu.yingxin.auth.dto.LoginRequest;
import com.sicnu.yingxin.auth.vo.LoginVO;

/**
 * 认证服务接口。
 */
public interface AuthService {

    /**
     * 账号密码登录。
     *
     * @param request 登录请求
     * @return 登录成功后的令牌信息
     */
    LoginVO login(LoginRequest request);
}
