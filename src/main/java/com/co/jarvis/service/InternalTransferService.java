package com.co.jarvis.service;

import com.co.jarvis.dto.UserDto;
import com.co.jarvis.dto.transfer.CashToBankTransferRequest;
import com.co.jarvis.dto.transfer.InternalTransferDto;
import com.co.jarvis.enums.EInternalTransferStatus;
import com.co.jarvis.enums.EInternalTransferType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface InternalTransferService {

    /**
     * Registra una consignación de efectivo desde la caja hacia un banco.
     * @param supportFile archivo soporte opcional (pdf/png/jpeg/webp, &lt;= 5 MB).
     */
    InternalTransferDto transferCashToBank(CashToBankTransferRequest request,
                                           MultipartFile supportFile,
                                           UserDto user) throws IOException;

    InternalTransferDto getById(String id);

    /**
     * Devuelve los bytes del archivo soporte asociado a un traslado.
     */
    byte[] getSupport(String id) throws IOException;

    /**
     * Devuelve el content-type almacenado o detectado del archivo soporte.
     */
    String getSupportContentType(String id);

    /**
     * Anula un traslado previamente registrado. El efectivo "regresa" virtualmente
     * a la caja (el arqueo deja de contarlo como egreso).
     */
    InternalTransferDto cancel(String id, String reason, UserDto user);

    List<InternalTransferDto> list(LocalDate fromDate, LocalDate toDate,
                                    EInternalTransferType type,
                                    EInternalTransferStatus status);
}
