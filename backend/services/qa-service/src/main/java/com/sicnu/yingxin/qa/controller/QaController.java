package com.sicnu.yingxin.qa.controller;

import com.sicnu.yingxin.common.core.result.Result;
import com.sicnu.yingxin.qa.dto.AskRequest;
import com.sicnu.yingxin.qa.service.QaService;
import com.sicnu.yingxin.qa.vo.AskVO;
import com.sicnu.yingxin.qa.vo.HotQuestionVO;
import com.sicnu.yingxin.qa.vo.MessageVO;
import com.sicnu.yingxin.qa.vo.SessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 智能问答接口：会话、提问、历史消息、热门问题。
 */
@Tag(name = "智能问答", description = "问答会话与问答检索")
@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
public class QaController {

    private final QaService qaService;

    @Operation(summary = "创建问答会话", description = "须登录，会话绑定当前用户（渠道固定学生端网页）")
    @PostMapping("/sessions")
    public Result<SessionVO> createSession() {
        return Result.ok(qaService.createSession());
    }

    @Operation(summary = "提问", description = "热点缓存 → 问答库匹配 → 未命中转人工，返回答案与数字人播报信息；须登录")
    @PostMapping("/ask")
    public Result<AskVO> ask(@Valid @RequestBody AskRequest request) {
        return Result.ok(qaService.ask(request));
    }

    @Operation(summary = "查询会话消息", description = "按时间正序返回用户问句与数字人回答")
    @GetMapping("/sessions/{id}/messages")
    public Result<List<MessageVO>> listMessages(@PathVariable("id") Long sessionId) {
        return Result.ok(qaService.listMessages(sessionId));
    }

    @Operation(summary = "热门问题", description = "已发布条目按命中次数倒序取 5 条")
    @GetMapping("/hot")
    public Result<List<HotQuestionVO>> listHot() {
        return Result.ok(qaService.listHot());
    }
}
