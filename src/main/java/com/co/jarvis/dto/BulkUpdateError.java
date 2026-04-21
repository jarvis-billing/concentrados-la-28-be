package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Describes why a particular entry of a bulk price update could not be applied.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkUpdateError implements Serializable {

    private String productId;
    private String barcode;
    private String message;
}
