package com.sicnu.yingxin.auth.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 后台管理员账号（admin_user，admin 域一期表）：登录入口 type=2 使用。 */
@Data
@TableName("admin_user")
public class AdminUser {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;

    private String passwordHash;

    private String realName;

    /** 1 学工 / 2 后勤 / 3 保卫 / 4 学院 / 5 信息中心 */
    private Integer dept;

    /** 1 正常 / 2 禁用 */
    private Integer status;

    private LocalDateTime lastLoginTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableLogic(value = "0", delval = "id")
    @TableField(fill = FieldFill.INSERT)
    private Long deleted;
}
