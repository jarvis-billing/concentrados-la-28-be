package com.co.jarvis.dto.presale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreSaleNotification {

    private String preSaleId;
    private String preSaleNumber;
    private String sellerName;
    /** Nombre del cliente capturado por el vendedor (puede ser null) */
    private String clientName;
    private double totalAmount;
    private int itemCount;
    private LocalDateTime createdAt;
}
