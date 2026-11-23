package com.sicnu.yingxin.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sicnu.yingxin.auth.entity.AuthUserGroup;
import org.apache.ibatis.annotations.Mapper;

/** 用户-用户组关联数据访问。 */
@Mapper
public interface AuthUserGroupMapper extends BaseMapper<AuthUserGroup> {
}
