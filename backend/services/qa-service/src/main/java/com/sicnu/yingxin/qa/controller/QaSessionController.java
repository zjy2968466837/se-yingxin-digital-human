package com.sicnu.yingxin.qa.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sicnu.yingxin.common.core.result.Result;
import com.sicnu.yingxin.qa.dto.QaSessionCreateRequest;
import com.sicnu.yingxin.qa.service.QaSessionService;
import com.sicnu.yingxin.qa.vo.QaSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会话接口（S5 持久化样板）。
 *
 * <p>路径与网关一致：直连 8101 与经 8080 转发都是 {@code /api/qa/sessions}（网关不裁剪前缀）。
 */
@Tag(name = "问答会话（S5 样板）", description = "Controller → Service → Mapper → MySQL + Redis 的最小闭环；不含问答检索等 FR-3 业务逻辑")
@RestController
@RequestMapping("/api/qa/sessions")
public class QaSessionController {

    private final QaSessionService service;

    public QaSessionController(QaSessionService service) {
        this.service = service;
    }

    @Operation(summary = "创建会话", description = "写入 qa_session，雪花 ID 与审计字段由持久层自动填充")
    @PostMapping
    public Result<QaSessionVO> create(@Valid @RequestBody QaSessionCreateRequest request) {
        return Result.ok(service.create(request));
    }

    @Operation(summary = "分页查询会话", description = "MyBatis-Plus 分页；只返回 deleted = 0 的行，按 id 倒序")
    @GetMapping
    public Result<IPage<QaSessionVO>> page(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，1–100") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "按学生 userId 过滤，可不传") @RequestParam(required = false) Long userId) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return Result.ok(service.page(safePage, safeSize, userId));
    }

    @Operation(summary = "查询会话", description = "优先读 Redis 缓存，未命中回源 MySQL；不存在返回 2001")
    @GetMapping("/{id}")
    public Result<QaSessionVO> get(
            @Parameter(description = "会话 id（雪花 ID）", example = "3300000000000000001")
            @PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @Operation(summary = "删除会话", description = "逻辑删除：把 deleted 写成该行 id，并主动失效缓存")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "会话 id（雪花 ID）", example = "3300000000000000001")
            @PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
