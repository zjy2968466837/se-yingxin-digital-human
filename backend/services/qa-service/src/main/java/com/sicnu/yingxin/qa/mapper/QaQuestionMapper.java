package com.sicnu.yingxin.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sicnu.yingxin.qa.entity.QaQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 问答条目数据访问。
 */
@Mapper
public interface QaQuestionMapper extends BaseMapper<QaQuestion> {
}
