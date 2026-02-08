package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * DTO para agregar items a una factura de compra existente
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddItemsRequest implements Serializable {
    private List<PurchaseInvoiceItemDto> items;
}
