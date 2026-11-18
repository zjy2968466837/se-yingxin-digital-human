package com.sicnu.yingxin.checkin.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 材料清单项视图。
 */
@Data
@Builder
public class MaterialVO {

    /** 材料名称 */
    private String name;

    /** 是否必需 */
    private Boolean required;

    /** 补交说明 */
    private String remark;
}
