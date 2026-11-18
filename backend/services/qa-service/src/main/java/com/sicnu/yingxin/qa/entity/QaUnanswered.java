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
 * 未解答记录。
 */
@Data
@TableName("qa_unanswered")
public class QaUnanswered {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 原始问题 */
    private String question;

    /** 来源会话 */
    private Long sessionId;

    /** 用户 ID */
    private Long userId;

    /** 最高置信度 */
    private BigDecimal confidence;

    /** 1 待处理 / 2 已转人工 / 3 已补录问答库 / 4 已忽略 */
    private Integer status;

    /** 处理人 */
    private Long handlerId;

    /** 处理说明 */
    private String handleNote;

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
