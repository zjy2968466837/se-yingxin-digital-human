package com.sicnu.yingxin.checkin.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报到步骤管理视图（管理端，含启停状态）。
 */
@Data
@Builder
public class StepAdminVO {

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

    /** 启停开关：1 启用 / 0 停用 */
    private Integer enabled;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
