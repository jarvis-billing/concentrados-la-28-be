package com.co.jarvis.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Payload for bulk updating sale/cost prices at the presentation level.
 * Each entry targets a specific presentation by productId + barcode.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkPresentationPriceUpdateRequest implements Serializable {

    public static final int MAX_UPDATES_PER_REQUEST = 500;

    @Valid
    @NotEmpty(message = "Debe enviar al menos una actualización")
    @Size(max = MAX_UPDATES_PER_REQUEST,
            message = "Máximo " + MAX_UPDATES_PER_REQUEST + " actualizaciones por request")
    private List<PresentationPriceUpdate> updates;
}
