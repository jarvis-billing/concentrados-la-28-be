package com.co.jarvis.dto;

import com.co.jarvis.enums.EReturnStatus;
import com.co.jarvis.enums.EReturnType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnFilterDto {
    private EReturnType returnType;
    private EReturnStatus status;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String originalDocumentNumber;
    private String clientId;
    private String supplierId;
}
