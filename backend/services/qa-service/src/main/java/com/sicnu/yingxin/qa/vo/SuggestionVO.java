package com.sicnu.yingxin.qa.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推荐问题。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionVO {

    /** 问答条目 ID */
    private Long questionId;

    /** 标准问法 */
    private String question;
}
