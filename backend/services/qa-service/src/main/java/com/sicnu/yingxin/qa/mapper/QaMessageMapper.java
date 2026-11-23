package com.sicnu.yingxin.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sicnu.yingxin.qa.entity.QaMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 问答消息数据访问。
 */
@Mapper
public interface QaMessageMapper extends BaseMapper<QaMessage> {
}
