package com.co.jarvis.service;

import com.co.jarvis.dto.PurchaseFilterDto;
import com.co.jarvis.dto.PurchaseInvoiceDto;
import com.co.jarvis.dto.PurchaseInvoiceItemDto;

import java.util.List;

public interface PurchaseInvoiceService {
    
    List<PurchaseInvoiceDto> list(PurchaseFilterDto filter);
    
    PurchaseInvoiceDto findById(String id);
    
    PurchaseInvoiceDto create(PurchaseInvoiceDto dto);
    
    PurchaseInvoiceDto update(String id, PurchaseInvoiceDto dto);
    
    void deleteById(String id);
    
    /**
     * Agrega nuevos items a una factura de compra existente.
     * Los items existentes NO se modifican ni eliminan.
     * Recalcula el total de la factura.
     * @param id ID de la factura
     * @param newItems Lista de nuevos items a agregar
     * @return Factura actualizada con los nuevos items
     */
    PurchaseInvoiceDto addItems(String id, List<PurchaseInvoiceItemDto> newItems);
}
