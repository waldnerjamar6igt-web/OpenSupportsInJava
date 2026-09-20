package com.openSupports.demo.api.dto.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> content;
    private long totalElements;
    private int page;
    private int size;
    private int totalPages;

    public PageResult() {}

    public static <T> PageResult<T> of(List<T> content, long totalElements, int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.setContent(content);
        result.setTotalElements(totalElements);
        result.setPage(page);
        result.setSize(size);
        result.setTotalPages(size > 0 ? (int) Math.ceil((double) totalElements / size) : 0);
        return result;
    }
}
