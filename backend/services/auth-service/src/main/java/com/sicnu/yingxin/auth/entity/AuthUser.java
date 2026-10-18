package com.sicnu.yingxin.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户账号表（规范 §4.1 auth_user）。
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

    private String passwordHash;

    /** 1 正常 / 2 禁用 / 3 未激活 */
    private Integer status;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    @TableLogic
    private Integer deleted;
}
