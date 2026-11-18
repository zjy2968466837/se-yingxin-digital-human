package com.sicnu.yingxin.qa.service;

import com.sicnu.yingxin.qa.dto.AskRequest;
import com.sicnu.yingxin.qa.vo.AskVO;
import com.sicnu.yingxin.qa.vo.HotQuestionVO;
import com.sicnu.yingxin.qa.vo.MessageVO;
import com.sicnu.yingxin.qa.vo.SessionVO;

import java.util.List;

/**
 * 智能问答服务：会话管理、问答检索、热门问题。
 */
public interface QaService {

    /**
     * 创建问答会话（须登录，渠道固定为 1 学生端网页）。
     */
    SessionVO createSession();

    /**
     * 提问：热点缓存 → 问答库打分 → 未命中转人工记录。
     */
    AskVO ask(AskRequest request);

    /**
     * 查询会话消息，按时间正序。
     */
    List<MessageVO> listMessages(Long sessionId);

    /**
     * 热门问题 TOP5（已发布条目按命中次数倒序）。
     */
    List<HotQuestionVO> listHot();
}
