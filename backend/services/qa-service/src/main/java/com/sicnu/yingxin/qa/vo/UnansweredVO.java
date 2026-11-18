package com.sicnu.yingxin.qa.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 未解答记录管理端视图。
 */
@Data
public class UnansweredVO {

    /** 未解答记录 ID */
    private Long id;

    /** 原始问题 */
    private String question;

    /** 来源会话 ID */
    private Long sessionId;

    /** 用户 ID */
    private Long userId;

    /** 最高置信度 */
    private BigDecimal confidence;

    /** 1 待处理 / 2 已转人工 / 3 已补录问答库 / 4 已忽略 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    public static UnansweredVO of(com.sicnu.yingxin.qa.entity.QaUnanswered entity) {
        UnansweredVO vo = new UnansweredVO();
        vo.setId(entity.getId());
        vo.setQuestion(entity.getQuestion());
        vo.setSessionId(entity.getSessionId());
        vo.setUserId(entity.getUserId());
        vo.setConfidence(entity.getConfidence());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
