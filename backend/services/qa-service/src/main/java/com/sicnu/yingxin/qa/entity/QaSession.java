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
 * 问答会话。
 */
@Data
@TableName("qa_session")
public class QaSession {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户 ID（qa_session.user_id NOT NULL，创建会话必须登录） */
    private Long userId;

    /** 渠道：1 学生端网页 / 2 管理后台，本服务固定 1 */
    private Integer channel;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 消息数 */
    private Integer messageCount;

    /** 满意度 */
    private Integer satisfied;

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
