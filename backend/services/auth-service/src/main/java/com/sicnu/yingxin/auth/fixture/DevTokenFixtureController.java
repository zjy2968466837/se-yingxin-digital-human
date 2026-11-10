package com.sicnu.yingxin.auth.fixture;

import com.sicnu.yingxin.auth.service.JwtService;
import com.sicnu.yingxin.auth.service.SessionService;
import com.sicnu.yingxin.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 【脚手架验证夹具，不是业务功能】直接签发任意 {@code userId}/{@code role} 的令牌，用于验证
 * 网关鉴权链路（H-04 / H-13）与"登出即失效"（F1.8）——**不校验密码、不对接身份源**。
 *
 * <p>业务登录（F1.1 学号密码登录 / F1.2 微信登录）在业务阶段按 FR-1 正式实现，实现后本夹具应删除。
 *
 * <p>三道护栏：
 * ① 默认**关闭**（{@code yingxin.auth.fixture.enabled=false}），只有本地联调才用
 * {@code AUTH_FIXTURE_ENABLED=true} 开启；<br>
 * ② 路径 {@code /api/dev/**} **不在网关白名单**，只能直连服务端口调用（8100），经网关一律 401；<br>
 * ③ 上云前必须确认该开关为 false（见 AGENTS.md 的"脚手架 / 业务落地"界碑）。
 */
@RestController
@RequestMapping("/api/dev")
@ConditionalOnProperty(prefix = "yingxin.auth.fixture", name = "enabled", havingValue = "true")
@Tag(name = "【开发夹具】令牌签发", description = "脚手架验证专用（默认关闭）；业务登录见 FR-1")
public class DevTokenFixtureController {

    private static final Logger log = LoggerFactory.getLogger(DevTokenFixtureController.class);

    private final JwtService jwtService;
    private final SessionService sessionService;

    public DevTokenFixtureController(JwtService jwtService, SessionService sessionService) {
        this.jwtService = jwtService;
        this.sessionService = sessionService;
    }

    @PostMapping("/token")
    @Operation(summary = "签发测试令牌（夹具）", description = "直接指定 userId 与 role 签发令牌并写入会话，用于验证网关鉴权")
    public Result<Map<String, Object>> issue(@RequestParam(defaultValue = "1") long userId,
                                             @RequestParam(defaultValue = "STUDENT") String role) {
        JwtService.IssuedToken issued = jwtService.issue(userId, role);
        sessionService.save(issued.jti(), userId, role, jwtService.getAccessTokenTtl());
        log.warn("⚠️ 使用开发夹具签发令牌 userId={} role={}（仅限本地联调）", userId, role);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", issued.token());
        body.put("tokenType", "Bearer");
        body.put("expiresIn", issued.expiresInSeconds());
        body.put("jti", issued.jti());
        body.put("userId", userId);
        body.put("role", role);
        return Result.ok(body);
    }

    @DeleteMapping("/session/{jti}")
    @Operation(summary = "删除会话（夹具）", description = "等价于登出：删除后该令牌立即失效（F1.8 的验证手段）")
    public Result<Void> revoke(@PathVariable String jti) {
        sessionService.remove(jti);
        log.warn("⚠️ 使用开发夹具删除会话 jti={}（仅限本地联调）", jti);
        return Result.ok();
    }
}
