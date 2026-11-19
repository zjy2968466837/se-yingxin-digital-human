package com.sicnu.yingxin.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * RBAC 跨表查询（user → usergroup → grouppriv → method）。
 * 仿禅道鉴权的核心判定：超级组 admin 免判直通，普通用户走四表联查。
 */
@Mapper
public interface RbacQueryMapper {

    @Select("SELECT COUNT(1) FROM auth_user_group ug "
            + "JOIN auth_group g ON g.id = ug.group_id AND g.deleted = 0 "
            + "WHERE ug.user_id = #{userId} AND g.code = 'admin' AND ug.deleted = 0")
    long inSuperGroup(@Param("userId") Long userId);

    @Select("SELECT DISTINCT m.code FROM auth_user_group ug "
            + "JOIN auth_group_priv gp ON gp.group_id = ug.group_id AND gp.deleted = 0 "
            + "JOIN auth_method m ON m.id = gp.method_id AND m.deleted = 0 "
            + "WHERE ug.user_id = #{userId} AND ug.deleted = 0")
    List<String> selectPermCodes(@Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM auth_user_group ug "
            + "JOIN auth_group_priv gp ON gp.group_id = ug.group_id AND gp.deleted = 0 "
            + "JOIN auth_method m ON m.id = gp.method_id AND m.deleted = 0 "
            + "WHERE ug.user_id = #{userId} AND m.code = #{methodCode} AND ug.deleted = 0")
    long countPerm(@Param("userId") Long userId, @Param("methodCode") String methodCode);

    @Select("SELECT g.code FROM auth_user_group ug "
            + "JOIN auth_group g ON g.id = ug.group_id AND g.deleted = 0 "
            + "WHERE ug.user_id = #{userId} AND ug.deleted = 0")
    List<String> selectGroupCodes(@Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM auth_user_group ug WHERE ug.group_id = #{groupId} AND ug.deleted = 0")
    long countMembers(@Param("groupId") Long groupId);
}
