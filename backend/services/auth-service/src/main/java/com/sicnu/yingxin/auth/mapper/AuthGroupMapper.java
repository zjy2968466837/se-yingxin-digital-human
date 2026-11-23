package com.sicnu.yingxin.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sicnu.yingxin.auth.entity.AuthGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 用户组数据访问。 */
@Mapper
public interface AuthGroupMapper extends BaseMapper<AuthGroup> {

    @Select("SELECT * FROM auth_group WHERE code = #{code} AND deleted = 0 LIMIT 1")
    AuthGroup selectByCode(@Param("code") String code);
}
