package com.sicnu.yingxin.qa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sicnu.yingxin.common.core.constant.CacheKeys;
import com.sicnu.yingxin.common.core.constant.Roles;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.core.result.PageResult;
import com.sicnu.yingxin.common.web.context.UserContextHolder;
import com.sicnu.yingxin.qa.dto.QuestionCreateRequest;
import com.sicnu.yingxin.qa.dto.QuestionUpdateRequest;
import com.sicnu.yingxin.qa.entity.QaQuestion;
import com.sicnu.yingxin.qa.entity.QaUnanswered;
import com.sicnu.yingxin.qa.mapper.QaQuestionMapper;
import com.sicnu.yingxin.qa.mapper.QaUnansweredMapper;
import com.sicnu.yingxin.qa.service.QaAdminService;
import com.sicnu.yingxin.qa.vo.QuestionAdminVO;
import com.sicnu.yingxin.qa.vo.UnansweredVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理端问答库服务实现。
 *
 * <p>所有操作要求管理员身份；每次增删改后用 SCAN 扫描 {@code yx:qa:hot:*}
 * 并逐个删除，实现「内容发布时失效」，保证用户端立刻读到新答案。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QaAdminServiceImpl implements QaAdminService {

    /** 状态：1 已发布 */
    private static final int STATUS_PUBLISHED = 1;

    private final QaQuestionMapper qaQuestionMapper;
    private final QaUnansweredMapper qaUnansweredMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public PageResult<QuestionAdminVO> pageQuestions(long page, long size, String keyword) {
        requireAdmin();
        LambdaQueryWrapper<QaQuestion> wrapper = new LambdaQueryWrapper<QaQuestion>()
                .orderByDesc(QaQuestion::getUpdateTime);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(QaQuestion::getQuestion, keyword.trim());
        }
        Page<QaQuestion> result = qaQuestionMapper.selectPage(Page.of(page, size), wrapper);
        List<QuestionAdminVO> list = result.getRecords().stream().map(QuestionAdminVO::of).toList();
        return PageResult.of(list, result.getTotal(), page, size);
    }

    @Override
    public Long createQuestion(QuestionCreateRequest request) {
        requireAdmin();
        QaQuestion question = new QaQuestion();
        question.setQuestion(request.getQuestion());
        question.setAnswer(request.getAnswer());
        question.setCategory(request.getCategory());
        question.setAnswerType(1);
        question.setSynonyms(request.getSynonyms());
        question.setTags(request.getTags());
        question.setStatus(STATUS_PUBLISHED);
        question.setHitCount(0L);
        question.setVersion(1);
        qaQuestionMapper.insert(question);
        invalidateHotCache();
        return question.getId();
    }

    @Override
    public void updateQuestion(Long id, QuestionUpdateRequest request) {
        requireAdmin();
        QaQuestion exist = qaQuestionMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "问答条目不存在");
        }
        // 传入空实体以触发持久层 update_time / update_by 自动填充
        LambdaUpdateWrapper<QaQuestion> wrapper = new LambdaUpdateWrapper<QaQuestion>()
                .eq(QaQuestion::getId, id)
                .setSql("version = version + 1");
        if (request.getQuestion() != null) {
            wrapper.set(QaQuestion::getQuestion, request.getQuestion());
        }
        if (request.getAnswer() != null) {
            wrapper.set(QaQuestion::getAnswer, request.getAnswer());
        }
        if (request.getCategory() != null) {
            wrapper.set(QaQuestion::getCategory, request.getCategory());
        }
        if (request.getSynonyms() != null) {
            wrapper.set(QaQuestion::getSynonyms, request.getSynonyms());
        }
        if (request.getTags() != null) {
            wrapper.set(QaQuestion::getTags, request.getTags());
        }
        if (request.getStatus() != null) {
            wrapper.set(QaQuestion::getStatus, request.getStatus());
        }
        qaQuestionMapper.update(new QaQuestion(), wrapper);
        invalidateHotCache();
    }

    @Override
    public void deleteQuestion(Long id) {
        requireAdmin();
        QaQuestion exist = qaQuestionMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "问答条目不存在");
        }
        qaQuestionMapper.deleteById(id);
        invalidateHotCache();
    }

    @Override
    public PageResult<UnansweredVO> pageUnanswered(Integer status, long page, long size) {
        requireAdmin();
        LambdaQueryWrapper<QaUnanswered> wrapper = new LambdaQueryWrapper<QaUnanswered>()
                .orderByDesc(QaUnanswered::getCreateTime);
        if (status != null) {
            wrapper.eq(QaUnanswered::getStatus, status);
        }
        Page<QaUnanswered> result = qaUnansweredMapper.selectPage(Page.of(page, size), wrapper);
        List<UnansweredVO> list = result.getRecords().stream().map(UnansweredVO::of).toList();
        return PageResult.of(list, result.getTotal(), page, size);
    }

    /** 管理端校验：未登录抛未授权，非管理员抛无权限 */
    private void requireAdmin() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!Roles.ADMIN.equals(UserContextHolder.getRole())) {
            throw new BizException(ErrorCode.NO_PERMISSION);
        }
    }

    /** 内容发布时失效：SCAN 匹配 yx:qa:hot:* 并逐个删除；失败只记日志，TTL 1 小时兜底 */
    private void invalidateHotCache() {
        try {
            List<String> keys = new ArrayList<>();
            ScanOptions options = ScanOptions.scanOptions().match(CacheKeys.QA_HOT + "*").count(500).build();
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                cursor.forEachRemaining(keys::add);
            }
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            log.info("问答库变更，热点缓存失效 count={}", keys.size());
        } catch (Exception e) {
            log.warn("热点缓存失效失败 reason={}", e.getMessage());
        }
    }
}
