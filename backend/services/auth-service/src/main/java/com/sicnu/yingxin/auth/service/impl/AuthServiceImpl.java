package com.sicnu.yingxin.auth.service.impl;

import com.sicnu.yingxin.auth.config.AuthRedisKey;
import com.sicnu.yingxin.auth.config.JwtProperties;
import com.sicnu.yingxin.auth.dto.LoginRequest;
import com.sicnu.yingxin.auth.entity.AdminUser;
import com.sicnu.yingxin.auth.entity.AuthUser;
import com.sicnu.yingxin.auth.error.AuthErrorCode;
import com.sicnu.yingxin.auth.mapper.AdminUserMapper;
import com.sicnu.yingxin.auth.mapper.AuthUserMapper;
import com.sicnu.yingxin.auth.service.AuthService;
import com.sicnu.yingxin.auth.vo.LoginVO;
import com.sicnu.yingxin.common.core.constant.AuthConstants;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.core.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;
    private final AuthUserMapper authUserMapper;
    private final AdminUserMapper adminUserMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginVO login(LoginRequest request) {
        Integer loginType = request.getLoginType();
        if (loginType == null) {
            loginType = 1;
        }

        Long userId;
        String role;
        switch (loginType) {
            case 1 -> {
                AuthUser user = authUserMapper.selectByStudentNo(request.getUsername());
                if (user == null) {
                    throw new BizException(AuthErrorCode.ACCOUNT_NOT_FOUND, "账号不存在");
                }
                validatePassword(request.getPassword(), user.getPasswordHash());
                if (user.getStatus() != null && user.getStatus() == 2) {
                    throw new BizException(ErrorCode.ACCOUNT_DISABLED);
                }
                userId = user.getId();
                role = AuthConstants.ROLE_STUDENT;
            }
            case 2 -> {
                AdminUser admin = adminUserMapper.selectByUsername(request.getUsername());
                if (admin == null) {
                    throw new BizException(AuthErrorCode.ACCOUNT_NOT_FOUND, "账号不存在");
                }
                validatePassword(request.getPassword(), admin.getPasswordHash());
                if (admin.getStatus() != null && admin.getStatus() == 2) {
                    throw new BizException(ErrorCode.ACCOUNT_DISABLED);
                }
                userId = admin.getId();
                role = AuthConstants.ROLE_ADMIN;
            }
            default -> throw new BizException(AuthErrorCode.LOGIN_TYPE_INVALID, "登录类型不合法");
        }

        String token = JwtUtil.generateToken(jwtProperties.getSecret(), userId, role, jwtProperties.getAccessTokenTtlHours());
        String jti = JwtUtil.parseToken(jwtProperties.getSecret(), token).getId();

        // 会话写入 Redis，TTL 与令牌一致（规范 §4.1）
        long ttlSeconds = Duration.ofHours(jwtProperties.getAccessTokenTtlHours()).getSeconds();
        redisTemplate.opsForValue().set(
                AuthRedisKey.session(jti),
                userId + ":" + role,
                ttlSeconds,
                TimeUnit.SECONDS);

        log.info("用户登录成功 userId={} role={}", userId, role);
        return new LoginVO(
                token,
                "Bearer",
                ttlSeconds,
                userId,
                role);
    }

    private void validatePassword(String raw, String hash) {
        if (hash == null || hash.isBlank()) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        if (!passwordEncoder.matches(raw, hash)) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
    }
}
