package com.sicnu.yingxin.common.core.result;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 统一分页外壳（规范 §3.2，H-01）。
 *
 * <pre>
 *   { "list": [ ... ], "total": 128, "page": 1, "size": 10 }
 * </pre>
 *
 * <p>放在 common-core 而非各服务：分页结构若各写一套，前端要为每个列表页适配不同字段名。
 *
 * <p>{@code list} 永不为 {@code null}，无数据时返回空集合，前端可直接遍历或映射。
 *
 * <p><b>与持久层的边界</b>：本类不依赖 MyBatis-Plus，从 {@code IPage} 到本类的转换在 Service 层完成
 * （规范 §3.2），避免持久层类型泄露到接口契约里。
 *
 * @param <T> 列表元素类型
 */
@Getter
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private final List<T> list;

    /** 总记录数 */
    private final long total;

    /** 当前页码，从 1 开始 */
    private final long page;

    /** 每页条数 */
    private final long size;

    @JsonCreator
    private PageResult(@JsonProperty("list") List<T> list,
                       @JsonProperty("total") long total,
                       @JsonProperty("page") long page,
                       @JsonProperty("size") long size) {
        this.list = list == null ? Collections.emptyList() : list;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> PageResult<T> of(List<T> list, long total, long page, long size) {
        return new PageResult<>(list, total, page, size);
    }

    /**
     * 元素类型转换（Entity / DTO → VO）。
     * 典型场景：查出实体列表后转成 VO 返回，分页元信息保持不变。
     */
    public static <S, T> PageResult<T> of(List<S> source, long total, long page, long size,
                                          Function<? super S, T> converter) {
        if (source == null) {
            return new PageResult<>(Collections.emptyList(), total, page, size);
        }
        List<T> converted = source.stream().map(converter).toList();
        return new PageResult<>(converted, total, page, size);
    }

    public static <T> PageResult<T> empty(long page, long size) {
        return new PageResult<>(Collections.emptyList(), 0L, page, size);
    }

    /** 是否还有下一页，供前端判断是否继续加载 */
    public boolean hasNext() {
        if (size <= 0) {
            return false;
        }
        return page * size < total;
    }
}
