package com.sicnu.yingxin.content.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量获取临时下载地址请求")
public class BatchUrlRequest {

    @Schema(description = "对象 key 列表（一次最多 50 个）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "keys 不能为空")
    private List<String> keys;
}
