package com.sicnu.yingxin.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sicnu.yingxin.qa.entity.QaSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * qa_session 持久层。复杂 SQL 写在 {@code resources/mapper/QaSessionMapper.xml}，不写在注解里。
 */
@Mapper
public interface QaSessionMapper extends BaseMapper<QaSession> {
}
