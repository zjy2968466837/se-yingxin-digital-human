package com.sicnu.yingxin.common.web.context;

import com.sicnu.yingxin.common.core.constant.AuthConstants;
import com.sicnu.yingxin.common.core.context.UserContext;
import com.sicnu.yingxin.common.core.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Servlet 侧用户上下文拦截器（规范 §4.3）。
 *
 * <p>从网关注入的固定 Header 中读取当前用户信息，写入线程本地变量，请求结束后清理。
 * 业务服务 Controller 中通过 {@link UserContextHolder} 获取用户 ID，禁止自行解析 JWT。
 */
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userIdHeader = request.getHeader(AuthConstants.HEADER_USER_ID);
        String role = request.getHeader(AuthConstants.HEADER_USER_ROLE);
        String traceId = request.getHeader(AuthConstants.HEADER_TRACE_ID);

        Long userId = null;
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                userId = Long.valueOf(userIdHeader);
            } catch (NumberFormatException ignored) {
                // 非法头按未登录处理
            }
        }

        UserContextHolder.set(new UserContext(userId, role, traceId));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }
}
