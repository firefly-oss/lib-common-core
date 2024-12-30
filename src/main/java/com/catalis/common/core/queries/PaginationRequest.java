package com.catalis.common.core.queries;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Represents a pagination request used for retrieving paginated results.
 * This class provides the page number and page size parameters necessary
 * for defining the segments of data to be requested.
 *
 * By default, the {@code pageNumber} is set to 0 and the {@code pageSize} is set to 10.
 * These defaults can be overridden by explicitly setting the values.
 *
 * This class includes a method to convert the pagination request into a {@link Pageable} object,
 * which is commonly used in Spring Data repositories to handle paging.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaginationRequest {

    private int pageNumber = 0;
    private int pageSize = 10;

    public Pageable toPageable() {
        return PageRequest.of(pageNumber, pageSize);
    }

}
