package com.co.jarvis.dto.cashregister;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSnapshotDto implements Serializable {

    private LocalDateTime snapshotAt;
    private String userId;
    private String userName;
    private BigDecimal totalCounted;
    private BigDecimal expectedTotal;
    private BigDecimal difference;
    private String reason;
}
