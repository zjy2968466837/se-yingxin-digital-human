package com.sicnu.yingxin.qa.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 问答消息。
 */
@Data
@TableName("qa_message")
public class QaMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 会话 ID */
    private Long sessionId;

    /** 1 用户 / 2 数字人 */
    private Integer role;

    /** 内容（加密存储） */
    private String content;

    /** 1 文本 / 2 富媒体 */
    private Integer contentType;

    /** 命中问答库条目 */
    private Long questionId;

    /** 置信度 0–100（落库存得分 ×100，返回前端时再除回 0–1） */
    private BigDecimal confidence;

    /** 是否 LLM 兜底：0 否 / 1 是 */
    private Integer llmUsed;

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
