package com.sicnu.yingxin.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sicnu.yingxin.qa.entity.QaSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 问答会话数据访问。
 */
@Mapper
public interface QaSessionMapper extends BaseMapper<QaSession> {
}
