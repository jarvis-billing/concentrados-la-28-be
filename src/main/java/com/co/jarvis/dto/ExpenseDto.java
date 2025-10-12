package com.co.jarvis.dto;

import com.co.jarvis.enums.EPaymentMethod;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDto implements Serializable {

    private String id;

    private OffsetDateTime dateTimeRecord;

    @NotNull(message = "amount: Requerido")
    @DecimalMin(value = "0.01", inclusive = true, message = "amount: Debe ser mayor a 0")
    private BigDecimal amount;

    @NotNull(message = "paymentMethod: Requerido")
    private EPaymentMethod paymentMethod;

    @NotBlank(message = "category: Requerido")
    @Size(max = 50, message = "category: Máximo 50 caracteres")
    private String category;

    @NotBlank(message = "description: Requerido")
    @Size(max = 200, message = "description: Máximo 200 caracteres")
    private String description;

    private String reference;

    private String source;

    private String createdBy;
}
