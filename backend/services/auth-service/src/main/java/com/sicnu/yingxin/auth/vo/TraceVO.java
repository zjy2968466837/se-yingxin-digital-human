package com.sicnu.yingxin.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 鉴权链路追踪项：当前用户对每个受保护路由的判定结果。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TraceVO {

    private String route;

    private String need;

    private boolean granted;
}
