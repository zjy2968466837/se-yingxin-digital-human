package com.sicnu.yingxin.qa.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新建问答条目请求。
 */
@Data
public class QuestionCreateRequest {

    /** 标准问法 */
    @NotBlank(message = "标准问法不能为空")
    private String question;

    /** 答案正文 */
    @NotBlank(message = "答案不能为空")
    private String answer;

    /** 知识域 */
    @NotBlank(message = "知识域不能为空")
    private String category;

    /** 同义词/口语化表达，逗号分隔，可空 */
    private String synonyms;

    /** 标签，可空 */
    private String tags;
}
