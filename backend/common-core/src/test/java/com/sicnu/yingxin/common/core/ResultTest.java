package com.sicnu.yingxin.common.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.core.error.ServiceSegment;
import com.sicnu.yingxin.common.core.result.PageResult;
import com.sicnu.yingxin.common.core.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S1 完成判定：单元测试可断言 Result 序列化结果与错误码（需求分析文档 §2.6.4）。
 */
class ResultTest {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("成功响应序列化为 {code:0, message:成功, data:{...}}")
    void successShape() throws Exception {
        String json = mapper.writeValueAsString(Result.ok(Map.of("service", "qa-service", "status", "UP")));
        Map<String, Object> parsed = mapper.readValue(json, MAP);
        assertThat(parsed.get("code")).isEqualTo(0);
        assertThat(parsed.get("message")).isEqualTo("成功");
        assertThat(parsed).containsKey("data");
        assertThat(parsed.keySet()).containsExactly("code", "message", "data");   // 不得出现 success 等多余字段
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) parsed.get("data");
        assertThat(data.get("service")).isEqualTo("qa-service");
    }

    @Test
    @DisplayName("失败响应的 data 字段必须存在且为 null（前端只判断 code）")
    void failKeepsNullDataField() throws Exception {
        String json = mapper.writeValueAsString(Result.fail(ErrorCode.RESOURCE_NOT_FOUND));
        assertThat(json)
                .contains("\"code\":2001")
                .contains("\"message\":\"资源不存在\"")
                .contains("\"data\":null");
        Map<String, Object> parsed = mapper.readValue(json, MAP);
        assertThat(parsed).containsKey("data");
        assertThat(parsed.get("data")).isNull();
    }

    @Test
    @DisplayName("分页外壳固定为 {list, total, page, size}")
    void pageResultShape() throws Exception {
        String json = mapper.writeValueAsString(Result.ok(PageResult.of(List.of("a", "b"), 42, 1, 10)));
        Map<String, Object> parsed = mapper.readValue(json, MAP);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) parsed.get("data");
        assertThat(data.keySet()).containsExactlyInAnyOrder("list", "total", "page", "size");
        assertThat(data.get("total")).isEqualTo(42);
    }

    @Test
    @DisplayName("错误码分段：全局码与各服务段位互不重叠，越段可被检出")
    void errorCodeSegments() {
        assertThat(ErrorCode.UNAUTHORIZED.getCode()).isBetween(1001, 1099);
        assertThat(ErrorCode.RESOURCE_NOT_FOUND.getCode()).isBetween(2001, 2099);
        assertThat(ErrorCode.PARAM_MISSING.getCode()).isBetween(3001, 3099);
        assertThat(ErrorCode.DEP_LLM.getCode()).isBetween(4001, 4099);
        assertThat(ErrorCode.SYSTEM_ERROR.getCode()).isBetween(9001, 9099);
        assertThat(ErrorCode.of(9999)).isEqualTo(ErrorCode.SYSTEM_ERROR);
        assertThat(ServiceSegment.QA.contains(2203)).isTrue();
        assertThat(ServiceSegment.QA.contains(2103)).isFalse();
        assertThat(ServiceSegment.AUTH.contains(2101)).isTrue();
    }

    @Test
    @DisplayName("业务异常不产生堆栈，code 与 message 原样带出")
    void bizExceptionCarriesCode() {
        BizException e = BizException.notFound("问答条目");
        assertThat(e.getCode()).isEqualTo(2001);
        assertThat(e.getMessage()).isEqualTo("问答条目不存在或已被删除");
        assertThat(e.getStackTrace()).isEmpty();
    }
}
