package com.sicnu.yingxin.checkin.controller;

import com.sicnu.yingxin.checkin.dto.StepCreateRequest;
import com.sicnu.yingxin.checkin.dto.StepUpdateRequest;
import com.sicnu.yingxin.checkin.service.CheckinService;
import com.sicnu.yingxin.checkin.vo.StepAdminVO;
import com.sicnu.yingxin.common.core.result.PageResult;
import com.sicnu.yingxin.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报到服务接口（管理端）。
 *
 * <p>所有接口要求当前用户为 ADMIN 角色，权限校验在 Service 层完成。
 * 管理操作为后台行为，不发布 MQ 埋点事件。
 */
@Tag(name = "报到管理", description = "报到步骤的查询、新建、修改（仅管理员）")
@RestController
@RequestMapping("/api/checkin/admin")
@RequiredArgsConstructor
public class CheckinAdminController {

    private final CheckinService checkinService;

    @Operation(summary = "分页查询步骤", description = "包含停用步骤，keyword 模糊匹配步骤名称")
    @GetMapping("/steps")
    public Result<PageResult<StepAdminVO>> pageSteps(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(checkinService.pageSteps(page, size, keyword));
    }

    @Operation(summary = "新建步骤", description = "step_code 全局唯一，重复返回 2302")
    @PostMapping("/steps")
    public Result<StepAdminVO> createStep(@Valid @RequestBody StepCreateRequest request) {
        return Result.ok(checkinService.createStep(request));
    }

    @Operation(summary = "修改步骤", description = "PATCH 语义：仅更新请求中出现的字段")
    @PatchMapping("/steps/{id}")
    public Result<StepAdminVO> updateStep(@PathVariable Long id,
                                          @Valid @RequestBody StepUpdateRequest request) {
        return Result.ok(checkinService.updateStep(id, request));
    }
}
