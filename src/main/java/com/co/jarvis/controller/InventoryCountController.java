package com.co.jarvis.controller;

import com.co.jarvis.dto.UserDto;
import com.co.jarvis.dto.inventorycount.*;
import com.co.jarvis.dto.inventorycount.HideUncountedResultDto;
import com.co.jarvis.dto.inventorycount.PhysicalInventoryValueReportDto;
import com.co.jarvis.dto.inventorycount.PhysicalInventoryValueReportFilter;
import com.co.jarvis.entity.InventoryCountSession;
import com.co.jarvis.service.InventoryCountService;
import com.co.jarvis.service.impl.InventoryCountServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping(value = "/api/inventory/count", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class InventoryCountController {

    private final InventoryCountService inventoryCountService;
    private final InventoryCountServiceImpl inventoryCountServiceImpl;

    /** Crear una nueva sesión de conteo */
    @PostMapping("/sessions")
    public ResponseEntity<InventoryCountSessionDto> createSession(Authentication auth) {
        log.info("InventoryCountController -> createSession");
        UserDto actor = (UserDto) auth.getPrincipal();
        InventoryCountSession session = inventoryCountService.createSession(actor.getFullName());
        return ResponseEntity.ok(inventoryCountServiceImpl.toSessionDto(session));
    }

    /** Sesión activa (IN_PROGRESS o PAUSED), si existe */
    @GetMapping("/sessions/active")
    public ResponseEntity<InventoryCountSessionDto> getActiveSession() {
        log.info("InventoryCountController -> getActiveSession");
        Optional<InventoryCountSession> active = inventoryCountService.findActiveSession();
        return active
                .map(s -> ResponseEntity.ok(inventoryCountServiceImpl.toSessionDto(s)))
                .orElse(ResponseEntity.noContent().build());
    }

    /** Detalle de una sesión */
    @GetMapping("/sessions/{id}")
    public ResponseEntity<InventoryCountSessionDto> getById(@PathVariable String id) {
        log.info("InventoryCountController -> getById: {}", id);
        return ResponseEntity.ok(inventoryCountServiceImpl.toSessionDto(inventoryCountService.getById(id)));
    }

    /** Listar sesiones con filtro de fecha opcional */
    @GetMapping("/sessions")
    public ResponseEntity<List<InventoryCountSessionDto>> listSessions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        log.info("InventoryCountController -> listSessions from={} to={}", fromDate, toDate);
        return ResponseEntity.ok(inventoryCountService.listSessions(fromDate, toDate));
    }

    /** Registrar o actualizar el conteo de una presentación */
    @PostMapping("/sessions/{id}/entries")
    public ResponseEntity<InventoryCountSessionDto> recordCount(
            @PathVariable String id,
            @RequestBody RecordCountRequest request,
            Authentication auth) {
        log.info("InventoryCountController -> recordCount session={} barcode={}", id, request.getBarcode());
        UserDto actor = (UserDto) auth.getPrincipal();
        InventoryCountSession updated = inventoryCountService.recordCount(id, request, actor.getFullName());
        return ResponseEntity.ok(inventoryCountServiceImpl.toSessionDto(updated));
    }

    /** Registrar conteo de múltiples presentaciones de un mismo producto de una sola vez */
    @PostMapping("/sessions/{id}/entries/bulk")
    public ResponseEntity<InventoryCountSessionDto> recordBulkCount(
            @PathVariable String id,
            @RequestBody BulkCountRequest request,
            Authentication auth) {
        log.info("InventoryCountController -> recordBulkCount session={} entries={}", id,
                request.getEntries() != null ? request.getEntries().size() : 0);
        UserDto actor = (UserDto) auth.getPrincipal();
        InventoryCountSession updated = inventoryCountService.recordBulkCount(id, request, actor.getFullName());
        return ResponseEntity.ok(inventoryCountServiceImpl.toSessionDto(updated));
    }

    /** Pausar una sesión */
    @PatchMapping("/sessions/{id}/pause")
    public ResponseEntity<InventoryCountSessionDto> pause(@PathVariable String id) {
        log.info("InventoryCountController -> pause: {}", id);
        return ResponseEntity.ok(inventoryCountServiceImpl.toSessionDto(inventoryCountService.pauseSession(id)));
    }

    /** Completar una sesión */
    @PatchMapping("/sessions/{id}/complete")
    public ResponseEntity<InventoryCountSessionDto> complete(@PathVariable String id, Authentication auth) {
        log.info("InventoryCountController -> complete: {}", id);
        UserDto actor = (UserDto) auth.getPrincipal();
        return ResponseEntity.ok(inventoryCountServiceImpl.toSessionDto(
                inventoryCountService.completeSession(id, actor.getFullName())));
    }

    /** Reporte completo: contados vs sin contar */
    @GetMapping("/sessions/{id}/report")
    public ResponseEntity<InventoryCountReportDto> getReport(@PathVariable String id) {
        log.info("InventoryCountController -> getReport: {}", id);
        return ResponseEntity.ok(inventoryCountService.getReport(id));
    }

    /**
     * Marca como inactivas (active=false) todas las presentaciones que NO fueron
     * contadas en la sesión indicada. Solo afecta presentaciones actualmente activas.
     * Las presentaciones inactivas quedan ocultas de búsquedas normales pero visibles
     * en reportes con filtro explícito.
     */
    @PostMapping("/sessions/{id}/hide-uncounted")
    public ResponseEntity<HideUncountedResultDto> hideUncounted(@PathVariable String id) {
        log.info("InventoryCountController -> hideUncounted: {}", id);
        return ResponseEntity.ok(inventoryCountService.hideUncountedPresentations(id));
    }

    /** Retorna los datos del reporte de valor de inventario como JSON. */
    @PostMapping("/report/value-data")
    public ResponseEntity<PhysicalInventoryValueReportDto> getValueReportData(
            @RequestBody PhysicalInventoryValueReportFilter filter) {
        log.info("InventoryCountController -> getValueReportData from={} to={}", filter.getFromDate(), filter.getToDate());
        return ResponseEntity.ok(inventoryCountService.getValueReportData(filter));
    }

    /**
     * Elimina una fila sin código de barras: borra el registro de conteo físico y
     * las presentaciones sin código del producto en el catálogo.
     */
    @DeleteMapping("/report/null-barcode-row")
    public ResponseEntity<Void> deleteNullBarcodeRow(
            @RequestParam String productId,
            @RequestParam String piId) {
        log.info("InventoryCountController -> deleteNullBarcodeRow productId={} piId={}", productId, piId);
        inventoryCountService.deleteNullBarcodeRow(productId, piId);
        return ResponseEntity.noContent().build();
    }

    /** Genera un PDF con el valor de inventario por conteo físico en el rango de fechas dado. */
    @PostMapping(value = "/report/value-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateValuePdf(@RequestBody PhysicalInventoryValueReportFilter filter) {
        log.info("InventoryCountController -> generateValuePdf from={} to={}", filter.getFromDate(), filter.getToDate());
        try {
            byte[] pdf = inventoryCountService.generateValuePdf(filter);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventario-valor.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Error generando reporte de valor de inventario", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
