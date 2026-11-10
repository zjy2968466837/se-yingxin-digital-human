package com.sicnu.yingxin.admin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员账号 admin_user（数据建模 §3.8 / DR-18）。
 *
 * <p>口令只存 BCrypt 散列（开发规范 §5.5），**严禁在本类或日志中输出明文**；
 * {@code deleted = 0} 表示未删除，删除时写入本行 id（数据建模 §1.3）。
 */
@Data
@TableName("admin_user")
public class AdminUser {

    /** 雪花 ID，非自增 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;

    /** BCrypt 散列，禁止明文 */
    private String passwordHash;

    private String realName;

    /** 1 学工 / 2 后勤 / 3 保卫 / 4 学院 / 5 信息中心（ACT-4） */
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

    @TableField(fill = FieldFill.INSERT)
    private Long deleted;
}
