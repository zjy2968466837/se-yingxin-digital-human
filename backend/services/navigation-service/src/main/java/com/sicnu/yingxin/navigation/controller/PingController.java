package com.sicnu.yingxin.navigation.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 连通性自检接口，用于验证服务本身与网关路由是否正常。
 */
@RestController
@RequestMapping("/api/navigation")
public class PingController {

    @Value("${spring.application.name}")
    private String serviceName;

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", serviceName);
        body.put("status", "UP");
        body.put("time", OffsetDateTime.now().toString());
        return body;
    }
}
