package com.sicnu.yingxin.common.web.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.sicnu.yingxin.common.web.context.UserContextHolder;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * 通用字段自动填充（开发规范 §5.2、数据建模 §1.3）：
 * {@code create_time / update_time / create_by / update_by / deleted} **由持久层填充，业务代码不得手写**。
 *
 * <p>字段名按实体属性（驼峰）取：{@code createTime}、{@code updateTime}、{@code createBy}、
 * {@code updateBy}、{@code deleted}。实体上需用 {@code @TableField(fill = FieldFill.INSERT)} 标注。
 */
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long operator = UserContextHolder.getUserIdOrSystem();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createBy", Long.class, operator);
        strictInsertFill(metaObject, "updateBy", Long.class, operator);
        strictInsertFill(metaObject, "deleted", Long.class, 0L);   // 0 = 未删除（BIGINT 语义见数据建模 §1.3）
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, "updateBy", Long.class, UserContextHolder.getUserIdOrSystem());
    }
}
