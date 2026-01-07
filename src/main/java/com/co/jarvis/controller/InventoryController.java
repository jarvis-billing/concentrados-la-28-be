package com.co.jarvis.controller;

import com.co.jarvis.dto.InventoryDashboardDto;
import com.co.jarvis.dto.StockAlertDto;
import com.co.jarvis.entity.InventoryAdjustment;
import com.co.jarvis.entity.InventoryMovement;
import com.co.jarvis.entity.PhysicalInventory;
import com.co.jarvis.enums.EAdjustmentReason;
import com.co.jarvis.enums.EAdjustmentType;
import com.co.jarvis.enums.EMovementType;
import com.co.jarvis.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/inventory", produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin(origins = "*")
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    @Autowired
    private InventoryService inventoryService;

    // ========== MOVIMIENTOS ==========

    @GetMapping("/movements")
    public ResponseEntity<List<InventoryMovement>> getMovements(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) EMovementType movementType,
            @RequestParam(required = false) String userId
    ) {
        logger.info("InventoryController -> getMovements");
        List<InventoryMovement> movements = inventoryService.getMovements(
                startDate, endDate, productId, movementType, userId);
        return ResponseEntity.ok(movements);
    }

    @GetMapping("/movements/product/{productId}")
    public ResponseEntity<List<InventoryMovement>> getMovementsByProduct(@PathVariable String productId) {
        logger.info("InventoryController -> getMovementsByProduct: {}", productId);
        List<InventoryMovement> movements = inventoryService.getMovementsByProduct(productId);
        return ResponseEntity.ok(movements);
    }

    @GetMapping("/movements/{id}")
    public ResponseEntity<InventoryMovement> getMovementById(@PathVariable String id) {
        logger.info("InventoryController -> getMovementById: {}", id);
        InventoryMovement movement = inventoryService.getMovementById(id);
        return ResponseEntity.ok(movement);
    }

    @PostMapping("/movements")
    public ResponseEntity<InventoryMovement> createMovement(@RequestBody InventoryMovement movement) {
        logger.info("InventoryController -> createMovement");
        InventoryMovement created = inventoryService.createMovement(movement);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ========== INVENTARIO FÍSICO ==========

    @GetMapping("/physical-count")
    public ResponseEntity<List<PhysicalInventory>> getPhysicalInventories(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) EAdjustmentReason adjustmentReason
    ) {
        logger.info("InventoryController -> getPhysicalInventories");
        List<PhysicalInventory> inventories = inventoryService.getPhysicalInventories(
                startDate, endDate, productId, adjustmentReason);
        return ResponseEntity.ok(inventories);
    }

    @GetMapping("/physical-count/{id}")
    public ResponseEntity<PhysicalInventory> getPhysicalInventoryById(@PathVariable String id) {
        logger.info("InventoryController -> getPhysicalInventoryById: {}", id);
        PhysicalInventory inventory = inventoryService.getPhysicalInventoryById(id);
        return ResponseEntity.ok(inventory);
    }

    @PostMapping("/physical-count")
    public ResponseEntity<PhysicalInventory> createPhysicalInventory(@RequestBody PhysicalInventory inventory) {
        logger.info("InventoryController -> createPhysicalInventory");
        PhysicalInventory created = inventoryService.createPhysicalInventory(inventory);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ========== AJUSTES ==========

    @GetMapping("/adjustments")
    public ResponseEntity<List<InventoryAdjustment>> getAdjustments(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) EAdjustmentType adjustmentType,
            @RequestParam(required = false) EAdjustmentReason reason
    ) {
        logger.info("InventoryController -> getAdjustments");
        List<InventoryAdjustment> adjustments = inventoryService.getAdjustments(
                startDate, endDate, productId, adjustmentType, reason);
        return ResponseEntity.ok(adjustments);
    }

    @GetMapping("/adjustments/{id}")
    public ResponseEntity<InventoryAdjustment> getAdjustmentById(@PathVariable String id) {
        logger.info("InventoryController -> getAdjustmentById: {}", id);
        InventoryAdjustment adjustment = inventoryService.getAdjustmentById(id);
        return ResponseEntity.ok(adjustment);
    }

    @PostMapping("/adjustments")
    public ResponseEntity<InventoryAdjustment> createAdjustment(@RequestBody InventoryAdjustment adjustment) {
        logger.info("InventoryController -> createAdjustment");
        InventoryAdjustment created = inventoryService.createAdjustment(adjustment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/adjustments/{id}/authorize")
    public ResponseEntity<InventoryAdjustment> authorizeAdjustment(
            @PathVariable String id,
            @RequestParam String authorizedBy
    ) {
        logger.info("InventoryController -> authorizeAdjustment: {}", id);
        InventoryAdjustment authorized = inventoryService.authorizeAdjustment(id, authorizedBy);
        return ResponseEntity.ok(authorized);
    }

    // ========== DASHBOARD ==========

    @GetMapping("/dashboard")
    public ResponseEntity<InventoryDashboardDto> getDashboard() {
        logger.info("InventoryController -> getDashboard");
        InventoryDashboardDto dashboard = inventoryService.getDashboard();
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/stock-alerts")
    public ResponseEntity<List<StockAlertDto>> getStockAlerts() {
        logger.info("InventoryController -> getStockAlerts");
        List<StockAlertDto> alerts = inventoryService.getStockAlerts();
        return ResponseEntity.ok(alerts);
    }

    // ========== REPORTES ==========

    @GetMapping("/reports/rotation")
    public ResponseEntity<Map<String, Object>> getRotationReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        logger.info("InventoryController -> getRotationReport");
        Map<String, Object> report = inventoryService.getRotationReport(startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/reports/valuation")
    public ResponseEntity<Map<String, Object>> getValuationReport() {
        logger.info("InventoryController -> getValuationReport");
        Map<String, Object> report = inventoryService.getValuationReport();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/reports/abc")
    public ResponseEntity<Map<String, Object>> getABCReport() {
        logger.info("InventoryController -> getABCReport");
        Map<String, Object> report = inventoryService.getABCReport();
        return ResponseEntity.ok(report);
    }
}
