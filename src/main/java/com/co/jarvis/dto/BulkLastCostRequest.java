package com.co.jarvis.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkLastCostRequest(
    @NotNull @Size(min = 1, max = 500)
    List<String> barcodes
) {}
