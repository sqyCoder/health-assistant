package com.health.assistant.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Getter;

import java.util.List;
import java.util.function.Function;

@Getter
public class PageResult<T> {

    private final long pageNum;
    private final long pageSize;
    private final long total;
    private final long pages;
    private final List<T> list;

    private PageResult(long pageNum, long pageSize, long total, long pages, List<T> list) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.pages = pages;
        this.list = list;
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), page.getRecords());
    }

    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> mapper) {
        List<T> mapped = page.getRecords().stream().map(mapper).toList();
        return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), mapped);
    }

    public static <T> PageResult<T> of(long pageNum, long pageSize, long total, List<T> list) {
        long pages = pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResult<>(pageNum, pageSize, total, pages, list);
    }
}
