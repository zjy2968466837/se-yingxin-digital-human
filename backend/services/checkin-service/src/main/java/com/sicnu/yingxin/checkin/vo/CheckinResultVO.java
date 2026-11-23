package com.sicnu.yingxin.checkin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 打卡结果视图（含幂等标记）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckinResultVO {

    /** 是否命中重复打卡（唯一键冲突，本次未产生新记录） */
    private Boolean isDuplicated;

    /** 步骤编码 */
    private String stepCode;

    /** 打卡时间（重复打卡时为已存在记录的时间） */
    private LocalDateTime checkinTime;
}
