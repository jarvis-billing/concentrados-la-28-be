package com.co.jarvis.controller;

import com.co.jarvis.dto.MerchandiseReturnDto;
import com.co.jarvis.dto.ReturnFilterDto;
import com.co.jarvis.dto.UserDto;
import com.co.jarvis.service.MerchandiseReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(value = "/api/merchandise-returns", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class MerchandiseReturnController {

    private final MerchandiseReturnService returnService;

    @PostMapping("/sale")
    public ResponseEntity<MerchandiseReturnDto> createSaleReturn(@RequestBody MerchandiseReturnDto dto) {
        log.info("MerchandiseReturnController -> createSaleReturn");
        String userId = extractUserId();
        return ResponseEntity.ok(returnService.createSaleReturn(dto, userId));
    }

    @PostMapping("/purchase")
    public ResponseEntity<MerchandiseReturnDto> createPurchaseReturn(@RequestBody MerchandiseReturnDto dto) {
        log.info("MerchandiseReturnController -> createPurchaseReturn");
        String userId = extractUserId();
        return ResponseEntity.ok(returnService.createPurchaseReturn(dto, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MerchandiseReturnDto> findById(@PathVariable String id) {
        log.info("MerchandiseReturnController -> findById: {}", id);
        return ResponseEntity.ok(returnService.findById(id));
    }

    @GetMapping("/number/{returnNumber}")
    public ResponseEntity<MerchandiseReturnDto> findByReturnNumber(@PathVariable String returnNumber) {
        log.info("MerchandiseReturnController -> findByReturnNumber: {}", returnNumber);
        return ResponseEntity.ok(returnService.findByReturnNumber(returnNumber));
    }

    @PostMapping("/list")
    public ResponseEntity<List<MerchandiseReturnDto>> list(@RequestBody(required = false) ReturnFilterDto filter) {
        log.info("MerchandiseReturnController -> list");
        return ResponseEntity.ok(returnService.list(filter));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<MerchandiseReturnDto> cancelReturn(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        log.info("MerchandiseReturnController -> cancelReturn: {}", id);
        String cancelReason = body.getOrDefault("cancelReason", "");
        String userId = extractUserId();
        return ResponseEntity.ok(returnService.cancelReturn(id, cancelReason, userId));
    }

    private String extractUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserDto userDto) {
                return userDto.getId();
            }
        } catch (Exception e) {
            log.warn("Could not extract userId from SecurityContext: {}", e.getMessage());
        }
        return null;
    }
}
