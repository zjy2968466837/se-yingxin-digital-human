package com.sicnu.yingxin.auth.vo;

import lombok.Data;

import java.util.List;

/** 登录成功响应：令牌 + 当前用户概况（组与权限点），前端据此渲染可用菜单。 */
@Data
public class LoginVO {

    private String accessToken;

    private String tokenType;

    /** 有效期（秒） */
    private Long expiresIn;

    private Long userId;

    private String role;

    private String realName;

    private List<GroupVO> groups;

    /** 权限点列表 module:priv；后台超级账号为 ALL */
    private List<String> perms;

    public LoginVO(String accessToken, String tokenType, Long expiresIn, Long userId, String role) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.role = role;
    }

    public LoginVO(String accessToken, String tokenType, Long expiresIn, Long userId, String role,
                   String realName, List<GroupVO> groups, List<String> perms) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.role = role;
        this.realName = realName;
        this.groups = groups;
        this.perms = perms;
    }
}
