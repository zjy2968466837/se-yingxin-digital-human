package com.sicnu.yingxin.qa.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数字人播报信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DigitalHumanVO {

    /** 播报形态：TWO_D 二维数字人 */
    private String mode;

    /** 播报台词，按段落拆分 */
    private List<String> lines;
}
