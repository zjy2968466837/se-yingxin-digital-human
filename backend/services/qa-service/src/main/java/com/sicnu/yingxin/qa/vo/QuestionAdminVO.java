package com.sicnu.yingxin.qa.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问答条目管理端视图。
 */
@Data
public class QuestionAdminVO {

    /** 问答条目 ID */
    private Long id;

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

    /** 命中次数 */
    private Long hitCount;

    /** 内容版本号 */
    private Integer version;

    /** 更新时间 */
    private LocalDateTime updateTime;

    public static QuestionAdminVO of(com.sicnu.yingxin.qa.entity.QaQuestion entity) {
        QuestionAdminVO vo = new QuestionAdminVO();
        vo.setId(entity.getId());
        vo.setQuestion(entity.getQuestion());
        vo.setAnswer(entity.getAnswer());
        vo.setCategory(entity.getCategory());
        vo.setSynonyms(entity.getSynonyms());
        vo.setTags(entity.getTags());
        vo.setStatus(entity.getStatus());
        vo.setHitCount(entity.getHitCount());
        vo.setVersion(entity.getVersion());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
