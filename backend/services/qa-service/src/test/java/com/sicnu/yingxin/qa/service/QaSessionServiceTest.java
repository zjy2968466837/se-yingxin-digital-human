package com.sicnu.yingxin.qa.service;

import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.qa.cache.QaSessionCache;
import com.sicnu.yingxin.qa.dto.QaSessionCreateRequest;
import com.sicnu.yingxin.qa.entity.QaSession;
import com.sicnu.yingxin.qa.mapper.QaSessionMapper;
import com.sicnu.yingxin.qa.vo.QaSessionVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 纯单元测试：不连 MySQL/Redis（开发规范 §6 测试分层）。
 */
@ExtendWith(MockitoExtension.class)
class QaSessionServiceTest {

    @Mock
    QaSessionMapper mapper;

    @Mock
    QaSessionCache cache;

    @InjectMocks
    QaSessionService service;

    private QaSessionCreateRequest request() {
        QaSessionCreateRequest request = new QaSessionCreateRequest();
        request.setUserId(1000123L);
        request.setChannel(1);
        return request;
    }

    @Test
    void create_fillsBusinessFieldsAndCaches() {
        QaSessionVO result = service.create(request());

        assertThat(result.getUserId()).isEqualTo(1000123L);
        assertThat(result.getChannel()).isEqualTo(1);
        assertThat(result.getMessageCount()).isZero();
        assertThat(result.getStartTime()).isNotNull();

        ArgumentCaptor<QaSession> captor = ArgumentCaptor.forClass(QaSession.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getDeleted()).isNull();   // 由 AuditMetaObjectHandler 填 0
        verify(cache).put(any(QaSessionVO.class));
    }

    @Test
    void get_returnsCachedValueWithoutQueryingDatabase() {
        QaSessionVO cached = new QaSessionVO();
        cached.setId(1L);
        when(cache.get(1L)).thenReturn(cached);

        assertThat(service.get(1L)).isSameAs(cached);
        verify(mapper, never()).selectOne(any());
    }

    @Test
    void get_fallsBackToDatabaseAndCachesResult() {
        QaSession entity = new QaSession();
        entity.setId(2L);
        entity.setUserId(1000123L);
        entity.setChannel(1);
        entity.setDeleted(0L);
        when(cache.get(2L)).thenReturn(null);
        when(mapper.selectOne(any())).thenReturn(entity);

        QaSessionVO result = service.get(2L);

        assertThat(result.getId()).isEqualTo(2L);
        verify(cache).put(any(QaSessionVO.class));
    }

    @Test
    void get_throwsNotFoundWhenRowMissing() {
        when(cache.get(3L)).thenReturn(null);
        when(mapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.get(3L))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode());
    }

    @Test
    void delete_writesRowIdIntoDeletedAndEvictsCache() {
        QaSession entity = new QaSession();
        entity.setId(4L);
        entity.setDeleted(0L);
        when(mapper.selectOne(any())).thenReturn(entity);

        service.delete(4L);

        ArgumentCaptor<QaSession> captor = ArgumentCaptor.forClass(QaSession.class);
        verify(mapper).updateById(captor.capture());
        assertThat(captor.getValue().getDeleted()).isEqualTo(4L);
        verify(cache).evict(4L);
    }
}
