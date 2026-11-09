package com.co.jarvis.service;

import com.co.jarvis.dto.SupplierPaymentDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface SupplierPaymentService {
    SupplierPaymentDto create(SupplierPaymentDto dto, MultipartFile support) throws IOException;

    /**
     * Valida los datos de un pago sin guardarlo (lanza FieldsException si hay errores).
     */
    void validatePayment(SupplierPaymentDto dto);
    List<SupplierPaymentDto> list(String supplierId, LocalDate from, LocalDate to,
                                   String status, Boolean unlinkedOnly, String bankAccountId);
    byte[] getSupport(String id) throws IOException;
    String getSupportContentType(String id) throws IOException;
}
