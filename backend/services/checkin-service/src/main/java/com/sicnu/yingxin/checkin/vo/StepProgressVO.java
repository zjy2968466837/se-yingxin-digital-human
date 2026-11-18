package com.sicnu.yingxin.checkin.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单个步骤的报到进度项。
 */
@Data
@Builder
public class StepProgressVO {

    /** 步骤编码 */
    private String stepCode;

    /** 步骤名称 */
    private String stepName;

    /** 展示顺序号 */
    private Integer seq;

    /** 状态：1 待办 / 2 已完成 / 3 不适用 */
    private Integer status;

    /** 打卡时间，未完成为 null */
    private LocalDateTime checkinTime;
}
