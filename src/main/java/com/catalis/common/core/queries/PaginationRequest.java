package com.catalis.common.core.queries;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
