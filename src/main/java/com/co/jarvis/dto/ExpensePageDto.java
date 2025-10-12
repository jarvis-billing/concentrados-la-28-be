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
public class ExpensePageDto {
    private List<ExpenseDto> items;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
}
