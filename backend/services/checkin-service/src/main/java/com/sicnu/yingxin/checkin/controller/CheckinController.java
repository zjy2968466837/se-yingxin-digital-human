package com.sicnu.yingxin.checkin.controller;

import com.sicnu.yingxin.checkin.service.CheckinService;
import com.sicnu.yingxin.checkin.vo.CheckinResultVO;
import com.sicnu.yingxin.checkin.vo.MaterialVO;
import com.sicnu.yingxin.checkin.vo.ProgressVO;
import com.sicnu.yingxin.checkin.vo.StepVO;
import com.sicnu.yingxin.checkin.vo.VoucherVO;
import com.sicnu.yingxin.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 报到服务接口（学生端）。
 *
 * <p>网关不裁前缀，映射必须带完整 /api/checkin 前缀。
 */
@Tag(name = "报到服务", description = "步骤引导、进度打卡、凭证领取")
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    @Operation(summary = "报到步骤列表", description = "仅返回启用步骤，按顺序号升序")
    @GetMapping("/steps")
    public Result<List<StepVO>> listSteps() {
        return Result.ok(checkinService.listEnabledSteps());
    }

    @Operation(summary = "步骤材料清单", description = "按展示顺序返回指定步骤需准备的材料")
    @GetMapping("/steps/{stepCode}/materials")
    public Result<List<MaterialVO>> listMaterials(@PathVariable String stepCode) {
        return Result.ok(checkinService.listMaterials(stepCode));
    }

    @Operation(summary = "我的报到进度", description = "以启用步骤为全集，返回各步骤状态与完成百分比")
    @GetMapping("/progress")
    public Result<ProgressVO> getProgress() {
        return Result.ok(checkinService.getProgress());
    }

    @Operation(summary = "步骤打卡", description = "幂等打卡：重复打卡返回 isDuplicated=true，不产生新记录")
    @PostMapping("/steps/{stepCode}/checkin")
    public Result<CheckinResultVO> checkin(@PathVariable String stepCode) {
        return Result.ok(checkinService.checkin(stepCode));
    }

    @Operation(summary = "领取报到凭证", description = "全部启用步骤完成后返回/签发凭证编号与二维码令牌")
    @GetMapping("/voucher")
    public Result<VoucherVO> getVoucher() {
        return Result.ok(checkinService.getVoucher());
    }
}
