package com.co.jarvis.enums;

public enum BatchStatus {
    ACTIVE,      // Lote activo con stock disponible
    DEPLETED,    // Lote agotado (stock = 0)
    EXPIRED,     // Lote con precio expirado (requiere actualización)
    CLOSED       // Lote cerrado manualmente
}
