package com.sicnu.yingxin.auth.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户账号（auth_user，数据建模 §auth）：学生 / 教职工统一账号，权限由用户组决定。
 * 敏感字段（phone / id_card）密文存储、禁止出接口。
 */
@Data
@TableName("auth_user")
public class AuthUser {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String studentNo;

    private String openid;

    private String unionid;

    private String realName;

    private String phone;

    private String idCard;

    private String passwordHash;

    /** 1 正常 / 2 禁用 / 3 未激活 */
    private Integer status;

    /** 0 待审批 / 1 通过 / 2 驳回（RBAC 注册审批流） */
    private Integer approveStatus;

    private LocalDateTime lastLoginTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /** 逻辑删除：0=未删除；删除时写入本行 id（BIGINT 语义见数据建模 §1.3） */
    @TableLogic(value = "0", delval = "id")
    @TableField(fill = FieldFill.INSERT)
    private Long deleted;
}
