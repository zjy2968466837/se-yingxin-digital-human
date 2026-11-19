package com.sicnu.yingxin.auth.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 用户管理列表项。 */
@Data
public class UserAdminVO {

    private Long id;

    private String account;

    private String realName;

    /** 1 正常 / 2 禁用 */
    private Integer status;

    /** 0 待审批 / 1 通过 / 2 驳回 */
    private Integer approveStatus;

    private LocalDateTime lastLoginTime;

    private List<GroupVO> groups;
}
