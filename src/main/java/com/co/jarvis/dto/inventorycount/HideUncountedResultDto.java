package com.co.jarvis.dto.inventorycount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HideUncountedResultDto {

    /** Número de presentaciones que fueron marcadas como inactivas */
    private int hidden;

    /** Mensaje descriptivo del resultado */
    private String message;
}
