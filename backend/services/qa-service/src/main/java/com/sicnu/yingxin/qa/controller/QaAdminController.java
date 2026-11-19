package com.sicnu.yingxin.qa.controller;

import com.sicnu.yingxin.common.core.result.PageResult;
import com.sicnu.yingxin.common.core.result.Result;
import com.sicnu.yingxin.qa.dto.QuestionCreateRequest;
import com.sicnu.yingxin.qa.dto.QuestionUpdateRequest;
import com.sicnu.yingxin.qa.service.QaAdminService;
import com.sicnu.yingxin.qa.vo.QuestionAdminVO;
import com.sicnu.yingxin.qa.vo.UnansweredVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端问答库接口：条目 CRUD、未解答记录，仅管理员可用。
 */
@Tag(name = "问答库管理", description = "问答条目 CRUD 与未解答记录，仅管理员")
@RestController
@RequestMapping("/api/qa/admin")
@RequiredArgsConstructor
public class QaAdminController {

    private final QaAdminService qaAdminService;

    @Operation(summary = "分页查询问答条目", description = "keyword 对标准问法模糊匹配")
    @GetMapping("/questions")
    public Result<PageResult<QuestionAdminVO>> pageQuestions(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(qaAdminService.pageQuestions(page, size, keyword));
    }

    @Operation(summary = "新建问答条目", description = "新建即已发布，版本号 1")
    @PostMapping("/questions")
    public Result<Long> createQuestion(@Valid @RequestBody QuestionCreateRequest request) {
        return Result.ok(qaAdminService.createQuestion(request));
    }

    @Operation(summary = "局部更新问答条目", description = "只更新传入字段，版本号自增")
    @PatchMapping("/questions/{id}")
    public Result<Void> updateQuestion(@PathVariable("id") Long id,
                                       @RequestBody QuestionUpdateRequest request) {
        qaAdminService.updateQuestion(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除问答条目", description = "逻辑删除")
    @DeleteMapping("/questions/{id}")
    public Result<Void> deleteQuestion(@PathVariable("id") Long id) {
        qaAdminService.deleteQuestion(id);
        return Result.ok();
    }

    @Operation(summary = "分页查询未解答记录", description = "默认查待处理（status=1）")
    @GetMapping("/unanswered")
    public Result<PageResult<UnansweredVO>> pageUnanswered(
            @RequestParam(required = false, defaultValue = "1") Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(qaAdminService.pageUnanswered(status, page, size));
    }
}
