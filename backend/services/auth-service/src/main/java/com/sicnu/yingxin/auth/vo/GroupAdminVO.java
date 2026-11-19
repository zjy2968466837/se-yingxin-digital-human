package com.sicnu.yingxin.auth.vo;

import lombok.Data;

/** 用户组管理列表项。 */
@Data
public class GroupAdminVO {

    private Long id;

    private String code;

    private String name;

    private Boolean builtin;

    private long userCount;
}
