package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedAccountReport {

    private List<AccountSummary> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
