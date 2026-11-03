package com.co.jarvis.dto.cashregister;

import com.co.jarvis.enums.ECashCountMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCashCountRequest implements Serializable {
    private LocalDate sessionDate;
    private BigDecimal openingBalance;
    private List<CashDenominationDto> cashDenominations;

    /**
     * TOTAL: se usa totalCashCounted tal como lo digitó el operador.
     * DENOMINACIONES (o null): el total se calcula con cashDenominations.
     */
    private ECashCountMode countMode;
    private BigDecimal totalCashCounted;
    private String notes;
}
