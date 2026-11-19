package com.sicnu.yingxin.checkin.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 报到总进度视图。
 */
@Data
@Builder
public class ProgressVO {

    /** 各步骤进度，按 seq 升序 */
    private List<StepProgressVO> steps;

    /** 已完成步骤数 */
    private Integer doneCount;

    /** 启用步骤总数 */
    private Integer totalCount;

    /** 完成百分比（0-100 整数） */
    private Integer percent;
}
