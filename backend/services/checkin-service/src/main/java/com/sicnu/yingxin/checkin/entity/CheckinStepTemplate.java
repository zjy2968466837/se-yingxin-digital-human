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
 * 报到步骤模板（规范 §4.1 checkin_step_template）。
 */
@Data
@TableName("checkin_step_template")
public class CheckinStepTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 步骤编码，唯一 */
    private String stepCode;

    /** 步骤名称 */
    private String stepName;

    /** 展示顺序号 */
    private Integer seq;

    /** 适用人群（JSON 字符串） */
    private String applyScope;

    /** 办理地点 */
    private String location;

    /** 办公时间 */
    private String officeHours;

    /** 联系人电话 */
    private String contact;

    /** 启停开关：1 启用 / 0 停用 */
    private Integer enabled;

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
