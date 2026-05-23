package com.co.jarvis.controller;

import com.co.jarvis.config.PreSaleWebSocketHandler;
import com.co.jarvis.dto.UserDto;
import com.co.jarvis.dto.presale.*;
import com.co.jarvis.entity.PreSale;
import com.co.jarvis.enums.PreSaleStatus;
import com.co.jarvis.service.PreSaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(value = "/api/preventas", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PreSaleController {

    private final PreSaleService preSaleService;
    private final PreSaleWebSocketHandler webSocketHandler;

    @PostMapping
    public ResponseEntity<PreSaleDto> create(@RequestBody CreatePreSaleRequest request) {
        log.info("PreSaleController -> create");
        PreSale saved = preSaleService.create(request);
        webSocketHandler.broadcast(new WsMessage<>("PREVENTA_READY", toNotification(saved)));
        return ResponseEntity.ok(toDto(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PreSaleDto> findById(@PathVariable String id) {
        log.info("PreSaleController -> findById: {}", id);
        return ResponseEntity.ok(toDto(preSaleService.findById(id)));
    }

    @PostMapping("/list")
    public ResponseEntity<List<PreSaleDto>> list(@RequestBody(required = false) PreSaleFilterDto filter) {
        log.info("PreSaleController -> list");
        List<PreSaleDto> result = preSaleService.list(filter).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<PreSaleDto> cancel(@PathVariable String id, Authentication auth) {
        log.info("PreSaleController -> cancel: {}", id);
        UserDto actor = (UserDto) auth.getPrincipal();
        return ResponseEntity.ok(toDto(preSaleService.cancel(id, actor.getFullName())));
    }

    @PatchMapping("/{id}/billed")
    public ResponseEntity<PreSaleDto> markAsBilled(@PathVariable String id,
                                                    @RequestBody MarkBilledRequest body,
                                                    Authentication auth) {
        log.info("PreSaleController -> markAsBilled: {}", id);
        UserDto actor = (UserDto) auth.getPrincipal();
        return ResponseEntity.ok(toDto(preSaleService.markAsBilled(id, body.getBillingId(), actor.getFullName())));
    }

    @PatchMapping("/{id}/resend")
    public ResponseEntity<PreSaleDto> resend(@PathVariable String id) {
        log.info("PreSaleController -> resend: {}", id);
        PreSale ps = preSaleService.findById(id);
        if (ps.getStatus() != PreSaleStatus.PENDING) {
            return ResponseEntity.badRequest().build();
        }
        webSocketHandler.broadcast(new WsMessage<>("PREVENTA_READY", toNotification(ps)));
        return ResponseEntity.ok(toDto(ps));
    }

    private PreSaleDto toDto(PreSale preSale) {
        List<PreSaleItemDto> itemDtos = preSale.getItems() == null ? List.of() :
                preSale.getItems().stream()
                        .map(item -> PreSaleItemDto.builder()
                                .barcode(item.getBarcode())
                                .productId(item.getProductId())
                                .description(item.getDescription())
                                .saleType(item.getSaleType())
                                .unitMeasure(item.getUnitMeasure())
                                .presentationLabel(item.getPresentationLabel())
                                .price(item.getPrice())
                                .amount(item.getAmount())
                                .isBulk(item.isBulk())
                                .bulkInputAmount(item.getBulkInputAmount())
                                .subTotal(item.getSubTotal())
                                .build())
                        .collect(Collectors.toList());

        return PreSaleDto.builder()
                .id(preSale.getId())
                .preSaleNumber(preSale.getPreSaleNumber())
                .status(preSale.getStatus() != null ? preSale.getStatus().name() : null)
                .sellerName(preSale.getSellerName())
                .items(itemDtos)
                .totalAmount(preSale.getTotalAmount())
                .notes(preSale.getNotes())
                .createdAt(preSale.getCreatedAt())
                .finalizedAt(preSale.getFinalizedAt())
                .billedAt(preSale.getBilledAt())
                .billingId(preSale.getBillingId())
                .cancelledBy(preSale.getCancelledBy())
                .cancelledAt(preSale.getCancelledAt())
                .billedBy(preSale.getBilledBy())
                .build();
    }

    private PreSaleNotification toNotification(PreSale preSale) {
        return PreSaleNotification.builder()
                .preSaleId(preSale.getId())
                .preSaleNumber(preSale.getPreSaleNumber())
                .sellerName(preSale.getSellerName())
                .totalAmount(preSale.getTotalAmount())
                .itemCount(preSale.getItems() != null ? preSale.getItems().size() : 0)
                .createdAt(preSale.getCreatedAt())
                .build();
    }

}
