package com.sicnu.yingxin.checkin.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 报到步骤视图（学生端步骤列表项）。
 */
@Data
@Builder
public class StepVO {

    /** 步骤 ID */
    private Long id;

    /** 步骤编码 */
    private String stepCode;

    /** 步骤名称 */
    private String stepName;

    /** 展示顺序号 */
    private Integer seq;

    /** 办理地点 */
    private String location;

    /** 办公时间 */
    private String officeHours;

    /** 联系人电话 */
    private String contact;
}
