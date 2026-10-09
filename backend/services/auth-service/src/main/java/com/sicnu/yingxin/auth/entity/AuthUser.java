package com.sicnu.yingxin.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户账号（表 {@code auth_user}，见《数据建模》§3.1）。
 *
 * <p>注意：本项目不用 MyBatis-Plus 的 {@code @TableLogic}——逻辑删除语义是
 * 「{@code deleted = 0} 未删除，删除时写入本行 id」（数据建模 §1.3），
 * 因此**查询必须显式带 {@code deleted = 0}**。
 */
@TableName("auth_user")
public class AuthUser {

    public static final long NOT_DELETED = 0L;

    private Long id;
    private String studentNo;
    private String openid;
    private String unionid;
    private String realName;
    private String phone;
    private String idCard;
    private String passwordHash;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
    private Long deleted;

    /** 1 正常 / 2 禁用 / 3 未激活 */
    public boolean isActive() {
        return status != null && status == 1;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getUnionid() {
        return unionid;
    }

    public void setUnionid(String unionid) {
        this.unionid = unionid;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    public Long getDeleted() {
        return deleted;
    }

    public void setDeleted(Long deleted) {
        this.deleted = deleted;
    }
}
