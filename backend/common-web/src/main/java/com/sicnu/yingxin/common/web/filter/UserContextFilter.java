package com.sicnu.yingxin.common.web.filter;

import com.sicnu.yingxin.common.core.constant.HeaderNames;
import com.sicnu.yingxin.common.core.context.UserContext;
import com.sicnu.yingxin.common.web.context.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 用户上下文过滤器（H-05）：**只读**网关注入的 {@code X-User-Id} / {@code X-User-Role}，放进线程上下文。
 *
 * <p>业务服务禁止自行解析 JWT、禁止自行校验令牌；网关注入前会先删除入站同名头，防伪造。
 */
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Long userId = parseUserId(request.getHeader(HeaderNames.USER_ID));
        String role = request.getHeader(HeaderNames.USER_ROLE);
        String traceId = MDC.get(TraceIdFilter.MDC_TRACE_ID);
        UserContextHolder.set(userId == null
                ? UserContext.anonymous(traceId)
                : UserContext.of(userId, role, traceId));
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    private static Long parseUserId(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;   // 头内容非法按未登录处理，不抛异常（防止伪造头把请求打成 500）
        }
    }
}
