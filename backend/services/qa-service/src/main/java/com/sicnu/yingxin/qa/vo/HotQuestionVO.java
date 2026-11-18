package com.sicnu.yingxin.qa.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 热门问题。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotQuestionVO {

    /** 标准问法 */
    private String question;

    /** 命中次数 */
    private Long hitCount;
}
