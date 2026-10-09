package com.sicnu.yingxin.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sicnu.yingxin.auth.entity.AuthUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户账号 Mapper。分页与防全表更新由 {@code common-web} 的 MyBatis-Plus 配置统一提供。
 */
@Mapper
public interface AuthUserMapper extends BaseMapper<AuthUser> {
}
