package com.sicnu.yingxin.common.web;

import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.web.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 统一响应体在异常路径上的行为（需求分析文档 §2.6.1 判定 4、判定 6 的载体证明）：
 * 任何异常都必须返回 {@code {code, message, data}}，且 HTTP 状态码只表达传输语义。
 */
class GlobalExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    static class ProbeController {

        @GetMapping("/probe/biz")
        String biz() {
            throw BizException.notFound("问答条目");
        }

        @GetMapping("/probe/param")
        String param(@RequestParam String name) {
            return name;
        }

        @GetMapping("/probe/boom")
        String boom() {
            throw new IllegalStateException("数据库连接断了");
        }
    }

    @Test
    @DisplayName("业务异常：HTTP 200 + code 2001 + data 为 null，不含堆栈")
    void bizException() throws Exception {
        var response = mvc.perform(get("/probe/biz")).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString())
                .contains("\"code\":2001")
                .contains("问答条目不存在或已被删除")
                .contains("\"data\":null")
                .doesNotContain("BizException");
    }

    @Test
    @DisplayName("缺少必填参数：HTTP 400 + code 3001")
    void missingParam() throws Exception {
        var response = mvc.perform(get("/probe/param")).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("\"code\":3001").contains("name");
    }

    @Test
    @DisplayName("未捕获异常：HTTP 500 + code 9001，堆栈只进日志不进响应")
    void unexpected() throws Exception {
        var response = mvc.perform(get("/probe/boom")).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString())
                .contains("\"code\":9001")
                .doesNotContain("数据库连接断了");
    }
}
