package com.sicnu.yingxin.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import com.sicnu.yingxin.common.core.result.Result;

/**
 * 连通性自检接口：验证服务本身与网关路由，并确认统一响应体形态（code/message/data）。
 */
@Tag(name = "管理后台", description = "F11.2 连通性自检：验证服务启动、网关路由与统一响应体")
@RestController
@RequestMapping("/api/admin")
public class PingController {

    @Value("${spring.application.name}")
    private String serviceName;

    @Operation(summary = "连通性自检", description = "返回服务名、UP 状态与服务器时间")
    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", serviceName);
        body.put("status", "UP");
        body.put("time", OffsetDateTime.now().toString());
        return Result.ok(body);
    }
}
