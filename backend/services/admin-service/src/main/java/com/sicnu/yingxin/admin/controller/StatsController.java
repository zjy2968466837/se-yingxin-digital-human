package com.sicnu.yingxin.admin.controller;

import com.sicnu.yingxin.admin.service.DashboardService;
import com.sicnu.yingxin.admin.vo.OverviewVO;
import com.sicnu.yingxin.common.core.constant.Roles;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.core.result.Result;
import com.sicnu.yingxin.common.web.context.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计看板接口：仅后台管理员角色可访问。
 */
@Tag(name = "统计看板", description = "迎新数据驾驶舱")
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class StatsController {

    private final DashboardService dashboardService;

    @Operation(summary = "看板总览", description = "KPI + 分时报到趋势 + 学院进度 + 热门问题 + 问答命中分布")
    @GetMapping("/overview")
    public Result<OverviewVO> overview() {
        requireAdmin();
        return Result.ok(dashboardService.overview());
    }

    private void requireAdmin() {
        if (UserContextHolder.getUserId() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!Roles.ADMIN.equals(UserContextHolder.getRole())) {
            throw new BizException(ErrorCode.NO_PERMISSION, "仅管理员可查看统计看板");
        }
    }
}
