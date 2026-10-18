package com.sicnu.yingxin.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sicnu.yingxin.auth.entity.AuthUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户账号数据访问。
 */
@Mapper
public interface AuthUserMapper extends BaseMapper<AuthUser> {

    @Select("SELECT * FROM auth_user WHERE student_no = #{studentNo} AND deleted = 0 LIMIT 1")
    AuthUser selectByStudentNo(@Param("studentNo") String studentNo);
}
