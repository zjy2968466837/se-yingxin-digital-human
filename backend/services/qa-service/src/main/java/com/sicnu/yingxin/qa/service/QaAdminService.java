package com.sicnu.yingxin.qa.service;

import com.sicnu.yingxin.common.core.result.PageResult;
import com.sicnu.yingxin.qa.dto.QuestionCreateRequest;
import com.sicnu.yingxin.qa.dto.QuestionUpdateRequest;
import com.sicnu.yingxin.qa.vo.QuestionAdminVO;
import com.sicnu.yingxin.qa.vo.UnansweredVO;

/**
 * 管理端问答库服务：条目 CRUD、未解答记录分页。
 */
public interface QaAdminService {

    /**
     * 分页查询问答条目，keyword 对标准问法模糊匹配。
     */
    PageResult<QuestionAdminVO> pageQuestions(long page, long size, String keyword);

    /**
     * 新建问答条目（已发布、版本 1）。
     */
    Long createQuestion(QuestionCreateRequest request);

    /**
     * 局部更新问答条目，版本号自增。
     */
    void updateQuestion(Long id, QuestionUpdateRequest request);

    /**
     * 逻辑删除问答条目。
     */
    void deleteQuestion(Long id);

    /**
     * 分页查询未解答记录。
     */
    PageResult<UnansweredVO> pageUnanswered(Integer status, long page, long size);
}
