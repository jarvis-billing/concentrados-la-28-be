package com.co.jarvis.entity;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configuración del arqueo de caja (documento único).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "CASH_COUNT_CONFIG")
public class CashCountConfig {

    public static final String SINGLETON_ID = "default";
    public static final BigDecimal DEFAULT_THRESHOLD = BigDecimal.valueOf(5000);

    @Id
    private String id;

    /**
     * Si está activo, al cerrar un arqueo cuya diferencia (sobrante o faltante)
     * supere el umbral se exige una nota explicando la diferencia.
     */
    @Builder.Default
    private boolean requireNoteOnDifference = true;

    @Builder.Default
    private BigDecimal differenceThreshold = DEFAULT_THRESHOLD;

    private LocalDateTime updatedAt;
    private String updatedBy;
}
