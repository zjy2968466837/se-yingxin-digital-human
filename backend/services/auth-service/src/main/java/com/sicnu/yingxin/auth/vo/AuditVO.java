package com.sicnu.yingxin.auth.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 审计日志条目。 */
@Data
public class AuditVO {

    private String operator;

    private String action;

    private String target;

    private String detail;

    private LocalDateTime ts;
}
