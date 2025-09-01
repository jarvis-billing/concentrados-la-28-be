package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaginationDto<T> {

    private Integer pageSize;
    private Integer quantityPage;
    private Long totalElements;
    private Integer page;
    private List<T> content;

}
