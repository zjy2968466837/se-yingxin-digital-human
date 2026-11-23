package com.sicnu.yingxin.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sicnu.yingxin.admin.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * admin_user 持久层。按用户名查询走 {@code uk_username}，复杂 SQL 写在 resources/mapper 下。
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {
}
