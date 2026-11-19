package com.sicnu.yingxin.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sicnu.yingxin.auth.entity.AuthMethod;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 权限点数据访问。 */
@Mapper
public interface AuthMethodMapper extends BaseMapper<AuthMethod> {

    @Select("SELECT * FROM auth_method WHERE code = #{code} AND deleted = 0 LIMIT 1")
    AuthMethod selectByCode(@Param("code") String code);
}
