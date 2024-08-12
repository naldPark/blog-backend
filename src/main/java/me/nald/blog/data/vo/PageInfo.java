package me.nald.blog.data.vo;

import lombok.Data;

@Data
public class PageInfo {
    private long totalElements;
    private int totalPages;
    private int pageNumber;
    private int pageSize;
}
