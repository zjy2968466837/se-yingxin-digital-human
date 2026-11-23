package com.sicnu.yingxin.qa.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicnu.yingxin.common.core.constant.CacheKeys;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.web.context.UserContextHolder;
import com.sicnu.yingxin.common.web.event.EventPublisher;
import com.sicnu.yingxin.qa.dto.AskRequest;
import com.sicnu.yingxin.qa.entity.QaAskLog;
import com.sicnu.yingxin.qa.entity.QaMessage;
import com.sicnu.yingxin.qa.entity.QaQuestion;
import com.sicnu.yingxin.qa.entity.QaSession;
import com.sicnu.yingxin.qa.entity.QaUnanswered;
import com.sicnu.yingxin.qa.error.QaErrorCode;
import com.sicnu.yingxin.qa.mapper.QaAskLogMapper;
import com.sicnu.yingxin.qa.mapper.QaMessageMapper;
import com.sicnu.yingxin.qa.mapper.QaQuestionMapper;
import com.sicnu.yingxin.qa.mapper.QaSessionMapper;
import com.sicnu.yingxin.qa.mapper.QaUnansweredMapper;
import com.sicnu.yingxin.qa.service.QaService;
import com.sicnu.yingxin.qa.vo.AskVO;
import com.sicnu.yingxin.qa.vo.DigitalHumanVO;
import com.sicnu.yingxin.qa.vo.HotQuestionVO;
import com.sicnu.yingxin.qa.vo.MessageVO;
import com.sicnu.yingxin.qa.vo.SessionVO;
import com.sicnu.yingxin.qa.vo.SuggestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能问答服务实现。
 *
 * <p>检索链路：文本归一化 → 指纹查 Redis 热点缓存（命中类型 1）
 * → 问答库全量 2-gram Jaccard 打分（≥ 0.30 判命中，类型 2）
 * → 未命中记录 qa_unanswered（类型 4）。LLM 兜底（类型 3）本期未接入，保留枚举。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QaServiceImpl implements QaService {

    /** 会话渠道：1 学生端网页 / 2 管理后台，本服务固定 1 */
    private static final int CHANNEL_WEB = 1;

    /** 消息角色：1 用户 / 2 数字人 */
    private static final int ROLE_USER = 1;
    private static final int ROLE_BOT = 2;

    /** 命中类型 */
    private static final int HIT_CACHE = 1;
    private static final int HIT_KB = 2;
    private static final int HIT_MISS = 4;

    /** 问答库命中判定阈值 */
    private static final double HIT_THRESHOLD = 0.30;

    /** 热点缓存 TTL（秒）：1 小时 */
    private static final long HOT_TTL_SECONDS = 3600;

    /** 未命中固定话术 */
    private static final String MISS_ANSWER =
            "这个问题我还没有学会，已经记录下来转给工作人员处理。您可以先看看热门问题～";

    private final QaSessionMapper qaSessionMapper;
    private final QaMessageMapper qaMessageMapper;
    private final QaQuestionMapper qaQuestionMapper;
    private final QaUnansweredMapper qaUnansweredMapper;
    private final QaAskLogMapper qaAskLogMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final EventPublisher eventPublisher;
    private final QaTextMatcher textMatcher;

    @Override
    public SessionVO createSession() {
        // qa_session.user_id NOT NULL，创建会话必须登录
        Long userId = requireUserId();
        QaSession session = new QaSession();
        session.setUserId(userId);
        session.setChannel(CHANNEL_WEB);
        // start_time NOT NULL 且无默认值（main 表口径），创建即写当前时间
        session.setStartTime(java.time.LocalDateTime.now());
        session.setMessageCount(0);
        qaSessionMapper.insert(session);
        return new SessionVO(session.getId());
    }

    @Override
    public AskVO ask(AskRequest request) {
        long start = System.currentTimeMillis();
        // qa_session.user_id NOT NULL，提问同样要求登录
        requireUserId();
        if (request.getSessionId() == null) {
            throw new BizException(ErrorCode.PARAM_MISSING, "会话 ID 不能为空");
        }
        String rawQuestion = request.getQuestion();
        if (rawQuestion == null || rawQuestion.isBlank()) {
            throw new BizException(QaErrorCode.QUESTION_EMPTY, "问题内容为空");
        }
        String normalized = textMatcher.normalize(rawQuestion);
        if (normalized.isEmpty()) {
            throw new BizException(QaErrorCode.QUESTION_EMPTY, "问题内容为空");
        }

        // 会话必须存在，防止脏 sessionId 静默产生孤儿消息
        QaSession session = qaSessionMapper.selectById(request.getSessionId());
        if (session == null) {
            throw new BizException(QaErrorCode.SESSION_NOT_FOUND, "会话不存在");
        }

        Long userId = UserContextHolder.getUserId();
        String fingerprint = textMatcher.fingerprint(normalized);

        // 1. 热点缓存命中（指纹完全一致，置信度视为 1.00）
        CacheHit cached = readHotCache(fingerprint);
        int hitType;
        Long questionId = null;
        String answer;
        BigDecimal confidence = null;

        if (cached != null) {
            hitType = HIT_CACHE;
            questionId = cached.questionId();
            answer = cached.answer();
            confidence = BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        } else {
            // 2. 问答库全量打分：标准问法与每条同义词分别计算，取最大值
            Match best = matchKnowledgeBase(normalized);
            if (best != null && best.score() >= HIT_THRESHOLD) {
                hitType = HIT_KB;
                questionId = best.question().getId();
                answer = best.question().getAnswer();
                confidence = BigDecimal.valueOf(best.score()).setScale(2, RoundingMode.HALF_UP);
                qaQuestionMapper.update(null, new LambdaUpdateWrapper<QaQuestion>()
                        .eq(QaQuestion::getId, questionId)
                        .setSql("hit_count = hit_count + 1"));
                writeHotCache(fingerprint, questionId, answer);
            } else {
                // 4. 未命中：落未解答记录，由工作人员后续处理
                hitType = HIT_MISS;
                answer = MISS_ANSWER;
                QaUnanswered unanswered = new QaUnanswered();
        // status NOT NULL 无默认值（main 表口径）：1 待处理
        unanswered.setStatus(1);
                unanswered.setQuestion(rawQuestion.trim());
                unanswered.setSessionId(session.getId());
                unanswered.setUserId(userId);
                if (best != null) {
                    unanswered.setConfidence(BigDecimal.valueOf(best.score())
                            .setScale(2, RoundingMode.HALF_UP));
                }
                qaUnansweredMapper.insert(unanswered);
            }
        }

        // 用户问句与机器人回答各存一条消息
        insertMessage(session.getId(), ROLE_USER, rawQuestion, questionId, null);
        insertMessage(session.getId(), ROLE_BOT, answer, questionId, confidence);
        qaSessionMapper.update(null, new LambdaUpdateWrapper<QaSession>()
                .eq(QaSession::getId, session.getId())
                .setSql("message_count = message_count + 2"));

        // 提问日志（旁路，失败只记日志不打断主流程）
        long costMs = System.currentTimeMillis() - start;
        try {
            QaAskLog askLog = new QaAskLog();
            askLog.setUserId(userId);
            askLog.setQuestionHash(fingerprint);
            askLog.setHitType(hitType);
            askLog.setCostMs((int) costMs);
            askLog.setSessionId(session.getId());
            qaAskLogMapper.insert(askLog);
        } catch (Exception e) {
            log.warn("提问日志写入失败 sessionId={} reason={}", session.getId(), e.getMessage());
        }

        // 埋点事件：1 问答 / 渠道 1 学生端网页，ext 带 hitType 与耗时
        eventPublisher.publish(EventType.QUESTION, userId, "qa", questionId,
                CHANNEL_WEB, java.util.Map.of("hitType", hitType, "costMs", costMs));

        // 未命中时给 3 条热门问题引导用户
        List<SuggestionVO> suggestions = hitType == HIT_MISS ? hotSuggestions() : List.of();
        return new AskVO(answer, hitType, questionId, confidence, suggestions, costMs,
                new DigitalHumanVO("TWO_D", List.of(answer)));
    }

    @Override
    public List<MessageVO> listMessages(Long sessionId) {
        QaSession session = qaSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(QaErrorCode.SESSION_NOT_FOUND, "会话不存在");
        }
        List<QaMessage> messages = qaMessageMapper.selectList(new LambdaQueryWrapper<QaMessage>()
                .eq(QaMessage::getSessionId, sessionId)
                .orderByAsc(QaMessage::getId));
        List<MessageVO> result = new ArrayList<>(messages.size());
        for (QaMessage message : messages) {
            result.add(new MessageVO(message.getRole(), message.getContent(),
                    message.getCreateTime(), message.getQuestionId()));
        }
        return result;
    }

    @Override
    public List<HotQuestionVO> listHot() {
        List<QaQuestion> questions = qaQuestionMapper.selectList(new LambdaQueryWrapper<QaQuestion>()
                .eq(QaQuestion::getStatus, 1)
                .orderByDesc(QaQuestion::getHitCount)
                .last("LIMIT 5"));
        List<HotQuestionVO> result = new ArrayList<>(questions.size());
        for (QaQuestion question : questions) {
            result.add(new HotQuestionVO(question.getQuestion(), question.getHitCount()));
        }
        return result;
    }

    /** 未命中时的推荐：热门问题 TOP3 */
    private List<SuggestionVO> hotSuggestions() {
        List<QaQuestion> questions = qaQuestionMapper.selectList(new LambdaQueryWrapper<QaQuestion>()
                .eq(QaQuestion::getStatus, 1)
                .orderByDesc(QaQuestion::getHitCount)
                .last("LIMIT 3"));
        List<SuggestionVO> result = new ArrayList<>(questions.size());
        for (QaQuestion question : questions) {
            result.add(new SuggestionVO(question.getId(), question.getQuestion()));
        }
        return result;
    }

    /** 问答库全量打分，返回最高分条目；库为空返回 null */
    private Match matchKnowledgeBase(String normalizedInput) {
        List<QaQuestion> questions = qaQuestionMapper.selectList(new LambdaQueryWrapper<QaQuestion>()
                .eq(QaQuestion::getStatus, 1));
        Match best = null;
        for (QaQuestion question : questions) {
            double score = textMatcher.similarity(normalizedInput, textMatcher.normalize(question.getQuestion()));
            for (String synonym : splitSynonyms(question.getSynonyms())) {
                score = Math.max(score, textMatcher.similarity(normalizedInput, textMatcher.normalize(synonym)));
            }
            if (best == null || score > best.score()) {
                best = new Match(question, score);
            }
        }
        return best;
    }

    /** 同义词按逗号（中英文）切分并去空白 */
    private List<String> splitSynonyms(String synonyms) {
        List<String> result = new ArrayList<>();
        if (synonyms == null || synonyms.isBlank()) {
            return result;
        }
        for (String part : synonyms.split("[,，;；]")) {
            if (!part.isBlank()) {
                result.add(part.trim());
            }
        }
        return result;
    }

    /** 读热点缓存，解析失败视为未命中并记日志 */
    private CacheHit readHotCache(String fingerprint) {
        try {
            String json = redisTemplate.opsForValue().get(CacheKeys.qaHot(fingerprint));
            if (json == null || json.isBlank()) {
                return null;
            }
            JsonNode node = objectMapper.readTree(json);
            return new CacheHit(node.path("questionId").asLong(), node.path("answer").asText());
        } catch (Exception e) {
            log.warn("热点缓存读取失败，降级走问答库 fingerprint={} reason={}", fingerprint, e.getMessage());
            return null;
        }
    }

    /** 写热点缓存，TTL 1 小时；写失败不影响主流程 */
    private void writeHotCache(String fingerprint, Long questionId, String answer) {
        try {
            String json = objectMapper.writeValueAsString(java.util.Map.of(
                    "questionId", questionId, "answer", answer));
            redisTemplate.opsForValue().set(CacheKeys.qaHot(fingerprint), json,
                    Duration.ofSeconds(HOT_TTL_SECONDS));
        } catch (Exception e) {
            log.warn("热点缓存写入失败 fingerprint={} reason={}", fingerprint, e.getMessage());
        }
    }

    private void insertMessage(Long sessionId, int role, String content, Long questionId, BigDecimal confidence) {
        QaMessage message = new QaMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setContentType(1);
        message.setQuestionId(questionId);
        // qa_message.confidence 语义为 0–100（DECIMAL(5,2)）：落库存得分 ×100，AskVO 返回前端仍为 0–1
        message.setConfidence(confidence == null ? null
                : confidence.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
        message.setLlmUsed(0);
        qaMessageMapper.insert(message);
    }

    /** 当前登录用户 ID，未登录抛未授权异常 */
    private Long requireUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    /** 热点缓存命中解析结果 */
    private record CacheHit(Long questionId, String answer) {
    }

    /** 问答库打分结果 */
    private record Match(QaQuestion question, double score) {
    }

    /** 事件类型常量（只引用需要的取值） */
    private static final class EventType {
        /** 1 问答 */
        static final int QUESTION = 1;

        private EventType() {
        }
    }
}
