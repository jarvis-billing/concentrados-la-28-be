package com.co.jarvis.entity;

import com.co.jarvis.enums.ECashLoanStatus;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "CASH_LOANS")
public class CashLoan {

    @Id
    private String id;

    private LocalDate loanDate;             // Fecha en que se tomó el préstamo
    private BigDecimal amount;              // Monto prestado
    private String borrower;               // Nombre de quien tomó el dinero
    private String reason;                 // Motivo del préstamo
    private String notes;                  // Notas adicionales

    @Builder.Default
    private ECashLoanStatus status = ECashLoanStatus.PENDIENTE;

    private LocalDate returnDate;          // Fecha en que se devolvió (null si pendiente)
    private BigDecimal returnedAmount;     // Monto devuelto (puede ser parcial en el futuro)
    private String returnNotes;            // Notas de la devolución

    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
