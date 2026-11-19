package com.sicnu.yingxin.checkin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sicnu.yingxin.checkin.entity.CheckinProgress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报到进度记录数据访问。
 */
@Mapper
public interface CheckinProgressMapper extends BaseMapper<CheckinProgress> {
}
