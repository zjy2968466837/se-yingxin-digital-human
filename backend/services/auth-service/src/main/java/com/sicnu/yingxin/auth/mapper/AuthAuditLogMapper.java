package com.sicnu.yingxin.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sicnu.yingxin.auth.entity.AuthAuditLog;
import org.apache.ibatis.annotations.Mapper;

/** 审计日志数据访问。 */
@Mapper
public interface AuthAuditLogMapper extends BaseMapper<AuthAuditLog> {
}
