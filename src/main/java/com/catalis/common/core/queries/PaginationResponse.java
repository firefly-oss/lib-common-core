package com.catalis.common.core.queries;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * A generic class representing a paginated response, typically used for returning
 * a portion of data along with pagination details in API responses.
 *
 * @param <T> the type of elements contained within the paginated content
 *
 * This class contains the following attributes:
 * - A list of elements (`content`) representing the data for the current page.
 * - The total number of elements across all pages (`totalElements`).
 * - The total number of pages (`totalPages`) based on the data size and page size.
 * - The current page number (`currentPage`), typically zero-based.
 *
 * This structure is useful for encapsulating paginated responses and providing
 * necessary metadata for navigation through paginated data.
 */
@Data
@AllArgsConstructor
@Builder
public class PaginationResponse <T>{
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
}