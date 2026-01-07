package com.co.jarvis.service;

import com.co.jarvis.dto.InventoryDashboardDto;
import com.co.jarvis.dto.StockAlertDto;
import com.co.jarvis.entity.InventoryAdjustment;
import com.co.jarvis.entity.InventoryMovement;
import com.co.jarvis.entity.PhysicalInventory;
import com.co.jarvis.enums.EAdjustmentReason;
import com.co.jarvis.enums.EAdjustmentType;
import com.co.jarvis.enums.EMovementType;

import java.util.List;
import java.util.Map;

public interface InventoryService {

    // ========== MOVIMIENTOS ==========
    List<InventoryMovement> getMovements(String startDate, String endDate, String productId, 
                                          EMovementType movementType, String userId);
    
    List<InventoryMovement> getMovementsByProduct(String productId);
    
    InventoryMovement getMovementById(String id);
    
    InventoryMovement createMovement(InventoryMovement movement);

    // ========== INVENTARIO FÍSICO ==========
    List<PhysicalInventory> getPhysicalInventories(String startDate, String endDate, 
                                                    String productId, EAdjustmentReason adjustmentReason);
    
    PhysicalInventory getPhysicalInventoryById(String id);
    
    PhysicalInventory createPhysicalInventory(PhysicalInventory inventory);

    // ========== AJUSTES ==========
    List<InventoryAdjustment> getAdjustments(String startDate, String endDate, String productId, 
                                              EAdjustmentType adjustmentType, EAdjustmentReason reason);
    
    InventoryAdjustment getAdjustmentById(String id);
    
    InventoryAdjustment createAdjustment(InventoryAdjustment adjustment);
    
    InventoryAdjustment authorizeAdjustment(String id, String authorizedBy);

    // ========== DASHBOARD ==========
    InventoryDashboardDto getDashboard();
    
    List<StockAlertDto> getStockAlerts();

    // ========== REPORTES ==========
    Map<String, Object> getRotationReport(String startDate, String endDate);
    
    Map<String, Object> getValuationReport();
    
    Map<String, Object> getABCReport();

    // ========== INTEGRACIÓN CON COMPRAS/VENTAS ==========
    void registerPurchaseMovement(String purchaseInvoiceId, String productId, Double quantity, 
                                   String presentationBarcode, String userId);
    
    void registerSaleMovement(String billingId, String productId, Double quantity, 
                               String presentationBarcode, String userId);
}
