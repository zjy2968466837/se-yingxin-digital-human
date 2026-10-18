package com.sicnu.yingxin.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sicnu.yingxin.auth.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 后台管理员账号数据访问。
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {

    @Select("SELECT * FROM admin_user WHERE username = #{username} AND deleted = 0 LIMIT 1")
    AdminUser selectByUsername(@Param("username") String username);
}
