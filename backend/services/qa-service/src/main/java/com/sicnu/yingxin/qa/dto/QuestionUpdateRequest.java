package com.sicnu.yingxin.qa.dto;

import lombok.Data;

/**
 * 局部更新问答条目请求，字段均可空，只更新传入的字段。
 */
@Data
public class QuestionUpdateRequest {

    /** 标准问法 */
    private String question;

    /** 答案正文 */
    private String answer;

    /** 知识域 */
    private String category;

    /** 同义词/口语化表达，逗号分隔 */
    private String synonyms;

    /** 标签 */
    private String tags;

    /** 1 已发布 / 2 草稿 / 3 停用 */
    private Integer status;
}
