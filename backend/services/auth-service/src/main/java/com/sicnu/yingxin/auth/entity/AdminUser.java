package com.sicnu.yingxin.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台管理员账号表（规范 §4.8 admin_user）。
 */
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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    @TableLogic
    private Integer deleted;
}
