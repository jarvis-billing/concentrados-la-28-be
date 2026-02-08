package com.co.jarvis.dto.batch;

import com.co.jarvis.entity.Batch;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BatchExpirationAlert {
    private Batch batch;
    private Integer daysUntilExpiration;
    private Boolean requiresAction;
    private String productDescription;
}
