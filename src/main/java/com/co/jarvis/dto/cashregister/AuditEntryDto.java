package com.co.jarvis.dto.cashregister;

import com.co.jarvis.enums.EAuditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntryDto implements Serializable {

    private String userId;
    private String userName;
    private EAuditAction action;
    private LocalDateTime timestamp;
    private String details;
}
