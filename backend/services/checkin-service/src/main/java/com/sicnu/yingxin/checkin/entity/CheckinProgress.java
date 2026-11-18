package com.sicnu.yingxin.checkin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报到进度记录（规范 §4.1 checkin_progress）。
 */
@Data
@TableName("checkin_progress")
public class CheckinProgress {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 学生用户 ID */
    private Long userId;

    /** 步骤编码 */
    private String stepCode;

    /** 状态：1 待办 / 2 已完成 / 3 不适用 */
    private Integer status;

    /** 打卡时间 */
    private LocalDateTime checkinTime;

    /** 打卡渠道：1 自助确认 / 2 现场扫码 */
    private Integer checkinChannel;

    /** 代打卡经办人 */
    private Long operatorId;

    /** 备注 */
    private String remark;

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
