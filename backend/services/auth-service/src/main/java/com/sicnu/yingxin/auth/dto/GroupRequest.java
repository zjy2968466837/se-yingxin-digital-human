package com.sicnu.yingxin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新建用户组请求。 */
@Data
public class GroupRequest {

    @NotBlank(message = "组编码不能为空")
    @Pattern(regexp = "[a-z][a-z0-9_]{1,19}", message = "组编码需以小写字母开头，仅含小写字母、数字、下划线，2-20 位")
    private String code;

    @NotBlank(message = "组名称不能为空")
    @Size(min = 2, max = 20, message = "组名称长度需在 2-20 位之间")
    private String name;
}
