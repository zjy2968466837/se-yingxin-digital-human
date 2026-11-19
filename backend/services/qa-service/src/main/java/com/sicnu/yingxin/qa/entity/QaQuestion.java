package com.sicnu.yingxin.qa.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问答条目。
 */
@Data
@TableName("qa_question")
public class QaQuestion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 标准问法 */
    private String question;

    /** 答案正文 */
    private String answer;

    /** 1 纯文本 / 2 富媒体 */
    private Integer answerType;

    /** 知识域 */
    private String category;

    /** 标签 */
    private String tags;

    /** 同义词/口语化表达，逗号分隔 */
    private String synonyms;

    /** 富媒体块定义（JSON） */
    private String media;

    /** 1 已发布 / 2 草稿 / 3 停用 */
    private Integer status;

    /** 命中次数 */
    private Long hitCount;

    /** 内容版本号 */
    private Integer version;

    /** 创建时间，持久层自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，持久层自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 id，持久层自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /** 修改人 id，持久层自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /** 逻辑删除：0=未删除；删除时写入本行 id，持久层自动填充 */
    @TableLogic(value = "0", delval = "id")
    @TableField(fill = FieldFill.INSERT)
    private Long deleted;
}
