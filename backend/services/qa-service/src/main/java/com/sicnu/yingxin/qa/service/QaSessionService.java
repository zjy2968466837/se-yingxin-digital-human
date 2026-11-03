package com.sicnu.yingxin.qa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.qa.cache.QaSessionCache;
import com.sicnu.yingxin.qa.dto.QaSessionCreateRequest;
import com.sicnu.yingxin.qa.entity.QaSession;
import com.sicnu.yingxin.qa.mapper.QaSessionMapper;
import com.sicnu.yingxin.qa.vo.QaSessionVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 会话服务（S5 纵向打样）。
 *
 * <p>范围刻意卡死：只演示 Controller → Service → Mapper → MySQL + Redis 的最小闭环，
 * **不实现 FR-3 的任何问答逻辑**（检索、意图识别、多轮对话都不在这里）。
 *
 * <p>三条持久化纪律：查询显式 {@code deleted = 0}；删除写本行 id 而不是 1；
 * id/审计字段全部交给 MyBatis-Plus 与 AuditMetaObjectHandler。
 */
@Service
public class QaSessionService {

    private final QaSessionMapper mapper;
    private final QaSessionCache cache;

    public QaSessionService(QaSessionMapper mapper, QaSessionCache cache) {
        this.mapper = mapper;
        this.cache = cache;
    }

    public QaSessionVO create(QaSessionCreateRequest request) {
        QaSession session = new QaSession();
        session.setUserId(request.getUserId());
        session.setChannel(request.getChannel());
        session.setStartTime(LocalDateTime.now());
        session.setMessageCount(0);
        mapper.insert(session);

        QaSessionVO vo = QaSessionVO.from(session);
        cache.put(vo);
        return vo;
    }

    public QaSessionVO get(Long id) {
        QaSessionVO cached = cache.get(id);
        if (cached != null) {
            return cached;
        }
        QaSession entity = selectActive(id);
        if (entity == null) {
            throw BizException.notFound("会话");
        }
        QaSessionVO vo = QaSessionVO.from(entity);
        cache.put(vo);
        return vo;
    }

    /**
     * 分页查询（演示用，走 MyBatis-Plus 分页插件）。
     *
     * <p>同样显式带 {@code deleted = 0}；userId 为空则不过滤。
     * 列表不读缓存——缓存只服务单条读取。
     */
    public IPage<QaSessionVO> page(int pageNo, int size, Long userId) {
        LambdaQueryWrapper<QaSession> wrapper = Wrappers.<QaSession>lambdaQuery()
                .eq(QaSession::getDeleted, 0L)
                .orderByDesc(QaSession::getId);
        if (userId != null) {
            wrapper.eq(QaSession::getUserId, userId);
        }
        return mapper.selectPage(new Page<>(pageNo, size), wrapper).convert(QaSessionVO::from);
    }

    public void delete(Long id) {
        if (selectActive(id) == null) {
            throw BizException.notFound("会话");
        }
        QaSession update = new QaSession();
        update.setId(id);
        update.setDeleted(id);   // 逻辑删除：写入本行 id；0 表示未删除
        mapper.updateById(update);
        cache.evict(id);
    }

    private QaSession selectActive(Long id) {
        return mapper.selectOne(Wrappers.<QaSession>lambdaQuery()
                .eq(QaSession::getId, id)
                .eq(QaSession::getDeleted, 0L));
    }
}
