package com.sicnu.yingxin.auth.vo;

import lombok.Data;

import java.util.List;

/** 当前登录用户信息（/api/auth/me）。 */
@Data
public class MeVO {

    private Long userId;

    private String account;

    private String realName;

    private String role;

    private List<GroupVO> groups;

    private List<String> perms;
}
