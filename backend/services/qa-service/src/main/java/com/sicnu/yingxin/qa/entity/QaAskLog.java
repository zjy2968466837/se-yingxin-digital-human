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
 * 提问日志。
 */
@Data
@TableName("qa_ask_log")
public class QaAskLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户 ID，匿名访问为空 */
    private Long userId;

    /** 问题指纹（归一化文本的 SHA-256） */
    private String questionHash;

    /** 1 缓存命中 / 2 问答库命中 / 3 LLM 兜底 / 4 未命中 */
    private Integer hitType;

    /** 响应耗时毫秒 */
    private Integer costMs;

    /** 关联会话 */
    private Long sessionId;

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
