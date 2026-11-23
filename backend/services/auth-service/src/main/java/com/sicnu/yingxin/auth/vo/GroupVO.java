package com.sicnu.yingxin.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 用户组简要信息。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupVO {

    private String code;

    private String name;
}
