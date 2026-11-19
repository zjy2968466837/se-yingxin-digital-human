package com.sicnu.yingxin.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sicnu.yingxin.qa.entity.QaAskLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提问日志数据访问。
 */
@Mapper
public interface QaAskLogMapper extends BaseMapper<QaAskLog> {
}
