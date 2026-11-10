package com.sicnu.yingxin.qa.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话表 qa_session。
 *
 * <p>本项目不使用 {@code @TableLogic}：{@code deleted = 0} 表示未删除，删除时写入本行 {@code id}
 * （数据建模 §1.3 / §5.1），这样唯一索引末位带 {@code deleted} 时仍可"删除后重建同业务键记录"。
 */
@Data
@TableName("qa_session")
public class QaSession {

    /** 雪花 ID，非自增（@TableId 不写会插入失败） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /** 1 学生端网页 / 2 管理后台，见数据建模附录 B.2 */
    private Integer channel;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer messageCount;

    /** 是否满意：0 未评价 / 1 满意 / 2 不满意 */
    private Integer satisfied;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT)
    private Long deleted;
}
