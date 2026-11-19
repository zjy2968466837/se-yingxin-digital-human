package com.sicnu.yingxin.checkin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 管理端修改报到步骤请求（PATCH，字段均可选，传了才更新）。
 */
@Data
public class StepUpdateRequest {

    /** 步骤名称 */
    private String stepName;

    /** 展示顺序号 */
    @Min(value = 0, message = "顺序号不能为负数")
    private Integer seq;

    /** 办理地点 */
    private String location;

    /** 办公时间 */
    private String officeHours;

    /** 联系人电话 */
    private String contact;

    /** 启停开关：1 启用 / 0 停用 */
    @Min(value = 0, message = "enabled 只能为 0 或 1")
    @Max(value = 1, message = "enabled 只能为 0 或 1")
    private Integer enabled;
}
