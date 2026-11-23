package com.sicnu.yingxin.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sicnu.yingxin.auth.entity.AuthModule;
import org.apache.ibatis.annotations.Mapper;

/** 权限模块数据访问。 */
@Mapper
public interface AuthModuleMapper extends BaseMapper<AuthModule> {
}
