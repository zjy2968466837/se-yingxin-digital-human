package com.sicnu.yingxin.common.web.filter;

import com.sicnu.yingxin.common.core.constant.HeaderNames;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪过滤器（H-06）：把网关生成的 traceId 放进日志上下文（MDC），并回写到响应头。
 *
 * <p>链路的三段一致性：网关生成 {@code X-Trace-Id} → 下游逐跳透传（Feign/RestTemplate 拦截器带）→
 * 每个服务的日志都含同一 traceId，9 个服务的日志可串成一条。
 * 直连服务（绕过网关）时本地生成一个，保证日志格式不缺字段。
 */
public class TraceIdFilter extends OncePerRequestFilter {

    /** 日志里的字段名，logback pattern 用 %X{traceId} 引用 */
    public static final String MDC_TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(HeaderNames.TRACE_ID);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(MDC_TRACE_ID, traceId);
        response.setHeader(HeaderNames.TRACE_ID, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }
}
