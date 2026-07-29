package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    /** Totales calculados sobre TODOS los registros que coinciden con el filtro (no solo la página actual) */
    private BigDecimal totalDebtGlobal;
    private BigDecimal totalPaidGlobal;
    private BigDecimal totalPendingGlobal;   // suma de currentBalance donde currentBalance > 0
    private long      pendingCountGlobal;    // cantidad de clientes con saldo > 0
}
