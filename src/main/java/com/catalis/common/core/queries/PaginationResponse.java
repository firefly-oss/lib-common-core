package com.catalis.common.core.queries;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class PaginationResponse <T>{
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
}
