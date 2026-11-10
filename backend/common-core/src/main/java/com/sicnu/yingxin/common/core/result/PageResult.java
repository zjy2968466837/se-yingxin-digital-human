package com.sicnu.yingxin.common.core.result;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页统一外壳（开发规范 §3.2）：{@code {list, total, page, size}}。
 *
 * <p>转换适配在 Service 层完成：Mapper 产出的是 MyBatis-Plus 的 {@code IPage}，
 * 不得直接返回给 Controller，统一在 Service 里转成 PageResult。
 */
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<T> list;
    /** 总记录数 */
    private long total;
    /** 当前页码，从 1 开始 */
    private long page;
    /** 每页条数 */
    private long size;

    public PageResult() {
    }

    public PageResult(List<T> list, long total, long page, long size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> PageResult<T> of(List<T> list, long total, long page, long size) {
        return new PageResult<>(list, total, page, size);
    }

    public static <T> PageResult<T> empty(long page, long size) {
        return new PageResult<>(Collections.emptyList(), 0L, page, size);
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
