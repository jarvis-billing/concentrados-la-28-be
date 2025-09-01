package com.co.jarvis.controller;

import com.co.jarvis.dto.BillingDto;
import com.co.jarvis.dto.BillingReportFilterDto;
import com.co.jarvis.dto.ProductSalesSummary;
import com.co.jarvis.service.SaleService;
import com.co.jarvis.util.exception.FieldsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

@RestController
@RequestMapping(value = "/api/sale", produces = MediaType.APPLICATION_JSON_VALUE)
public class SaleController extends GenericController<BillingDto, SaleService> {

    private static final Logger logger = LoggerFactory.getLogger(SaleController.class);

    @Autowired
    private SaleService service;

    @Override
    protected SaleService getService() {
        return service;
    }

    @GetMapping("/importOrdenToSale/{orderNumber}")
    public ResponseEntity<BillingDto> importOrdenToSale(@PathVariable Long orderNumber) throws FieldsException {
        logger.info("SaleController -> importOrdenToSale");
        BillingDto sale = service.importSaleOrder(orderNumber);
        return ResponseEntity.ok(sale);
    }

    @GetMapping("/lastBillingNumber")
    public ResponseEntity<Map<String, String>> getLastBillingNumber() {
        Map<String, String> response = new HashMap<>();
        response.put("billingNumber", service.getLastBillingNumber());

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/print/ticket-billing", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> printTicketBilling(@RequestBody BillingDto dto) {
        String fileName = format("factura_venta_%s.pdf", dto.getBillNumber());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData(fileName, fileName);
        byte[] billingTicket = service.printTicketBilling(dto);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .headers(headers)
                .body(billingTicket);
    }

    @GetMapping(value = "/find-billing/{billingNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BillingDto> findByBillingNumber(@PathVariable String billingNumber) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.findByBillingNumber(billingNumber));
    }

    @PostMapping(value = "/report/list-sales", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BillingDto>> findAllBilling(@RequestBody BillingReportFilterDto dto) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.findAllBilling(dto));
    }

    @PostMapping(value = "/report/product/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProductSalesSummary>> getProductSalesSummary(@RequestBody BillingReportFilterDto dto) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.getProductSalesSummary(dto));
    }
}
