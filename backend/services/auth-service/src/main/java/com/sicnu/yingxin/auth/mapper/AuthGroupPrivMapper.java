package com.sicnu.yingxin.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sicnu.yingxin.auth.entity.AuthGroupPriv;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 组授权表数据访问（禅道 grouppriv 语义）。 */
@Mapper
public interface AuthGroupPrivMapper extends BaseMapper<AuthGroupPriv> {

    @Select("SELECT mo.code FROM auth_group_priv gp "
            + "JOIN auth_method mo ON mo.id = gp.method_id AND mo.deleted = 0 "
            + "WHERE gp.group_id = #{groupId} AND gp.deleted = 0")
    List<String> selectMethodCodes(@Param("groupId") Long groupId);

    @Select("SELECT COUNT(1) FROM auth_group_priv gp WHERE gp.group_id = #{groupId} AND gp.deleted = 0")
    long countByGroup(@Param("groupId") Long groupId);

    /** 重新激活被逻辑删除的授权行（绕过 @TableLogic 的 deleted=0 过滤），避免与历史行撞唯一键。 */
    @Update("UPDATE auth_group_priv SET deleted = 0 "
            + "WHERE group_id = #{groupId} AND method_id = #{methodId} AND deleted != 0")
    int reactivate(@Param("groupId") Long groupId, @Param("methodId") Long methodId);
}
