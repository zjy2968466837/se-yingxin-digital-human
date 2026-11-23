package com.sicnu.yingxin.qa.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 提问响应。
 */
@Data
public class AskVO {

    /** 回答正文 */
    private String answer;

    /** 命中类型：1 缓存命中 / 2 问答库命中 / 4 未命中 */
    private Integer hitType;

    /** 命中的问答条目 ID，未命中为空 */
    private Long questionId;

    /** 置信度 0-1 两位小数，未命中为空 */
    private BigDecimal confidence;

    /** 推荐问题（未命中时给热门问题） */
    private List<SuggestionVO> suggestions;

    /** 响应耗时毫秒 */
    private Long costMs;

    /** 数字人播报信息 */
    private DigitalHumanVO digitalHuman;

    public AskVO(String answer, Integer hitType, Long questionId, BigDecimal confidence,
                 List<SuggestionVO> suggestions, Long costMs, DigitalHumanVO digitalHuman) {
        this.answer = answer;
        this.hitType = hitType;
        this.questionId = questionId;
        this.confidence = confidence;
        this.suggestions = suggestions;
        this.costMs = costMs;
        this.digitalHuman = digitalHuman;
    }
}
