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
 * 报到凭证（规范 §4.1 checkin_certificate）。
 */
@Data
@TableName("checkin_certificate")
public class CheckinCertificate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 学生用户 ID */
    private Long userId;

    /** 报到批次（签发时写入，「同批次一张有效凭证」的判定依据） */
    private Long batchId;

    /** 凭证编号，唯一 */
    private String certNo;

    /** 二维码令牌，唯一 */
    private String qrToken;

    /** 签发时间 */
    private LocalDateTime issuedTime;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 核销状态：1 未核销 / 2 已核销 / 3 已作废 */
    private Integer verifyStatus;

    /** 核销人 */
    private Long verifyBy;

    /** 核销时间 */
    private LocalDateTime verifyTime;

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
