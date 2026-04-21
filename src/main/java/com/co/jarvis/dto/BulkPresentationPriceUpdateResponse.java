package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Result of a bulk presentation price update.
 * {@code updated} is the number of presentations successfully modified,
 * {@code failed} is the number of entries that could not be applied (see {@link #errors}).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkPresentationPriceUpdateResponse implements Serializable {

    private int updated;
    private int failed;
    private List<BulkUpdateError> errors;
}
