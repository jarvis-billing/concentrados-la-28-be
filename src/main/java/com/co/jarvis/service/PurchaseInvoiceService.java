package com.co.jarvis.service;

import com.co.jarvis.dto.PurchaseFilterDto;
import com.co.jarvis.dto.CostHistoryEntry;
import com.co.jarvis.dto.PurchaseInvoiceDto;
import com.co.jarvis.dto.PurchaseInvoiceItemDto;
import com.co.jarvis.dto.PurchaseLastCostInfo;

import java.time.LocalDate;
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

    /**
     * Devuelve el costo de la última compra registrada para una presentación dada,
     * buscando el item más reciente con unitTotalCost != null.
     * @param presentationId barcode de la presentación
     * @return Info del último costo, o null si nunca ha sido comprado
     */
    PurchaseLastCostInfo getLastCost(String presentationId);

    /**
     * Devuelve el historial completo de compras de una presentación,
     * con los costos desglosados por unidad.
     * @param presentationId barcode de la presentación
     * @param fromDate filtro opcional: fecha mínima de factura (inclusive)
     * @param toDate filtro opcional: fecha máxima de factura (inclusive)
     * @return Lista ordenada de más reciente a más antiguo
     */
    List<CostHistoryEntry> getCostHistory(String presentationId, LocalDate fromDate, LocalDate toDate);
}
