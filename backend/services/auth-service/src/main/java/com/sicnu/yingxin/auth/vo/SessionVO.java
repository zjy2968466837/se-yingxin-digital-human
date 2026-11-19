package com.sicnu.yingxin.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 在线会话条目。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionVO {

    private String jti;

    private Long userId;

    private String account;

    private String role;

    /** 剩余有效期（秒） */
    private Long ttlSeconds;
}
