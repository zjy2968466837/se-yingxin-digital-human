package com.sicnu.yingxin.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sicnu.yingxin.qa.entity.QaUnanswered;
import org.apache.ibatis.annotations.Mapper;

/**
 * 未解答记录数据访问。
 */
@Mapper
public interface QaUnansweredMapper extends BaseMapper<QaUnanswered> {
}
