package com.sicnu.yingxin.checkin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端新建报到步骤请求。
 */
@Data
public class StepCreateRequest {

    /** 步骤编码，全局唯一 */
    @NotBlank(message = "步骤编码不能为空")
    private String stepCode;

    /** 步骤名称 */
    @NotBlank(message = "步骤名称不能为空")
    private String stepName;

    /** 展示顺序号 */
    @NotNull(message = "顺序号不能为空")
    private Integer seq;

    /** 办理地点 */
    private String location;

    /** 办公时间 */
    private String officeHours;

    /** 联系人电话 */
    private String contact;
}
