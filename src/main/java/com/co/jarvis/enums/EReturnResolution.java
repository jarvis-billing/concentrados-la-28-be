package com.co.jarvis.enums;

public enum EReturnResolution {
    // Para devoluciones de venta
    NOTA_CREDITO,             // Genera saldo a favor para el cliente
    REEMBOLSO_EFECTIVO,       // Reembolso en efectivo al cliente
    REEMBOLSO_TRANSFERENCIA,  // Reembolso por transferencia bancaria al cliente
    CAMBIO_PRODUCTO,          // Cambio por otro producto (nueva venta separada)

    // Para devoluciones de compra
    ABONO_PROVEEDOR,          // Nota crédito del proveedor (abona contra futuras facturas)
    REEMBOLSO_PROVEEDOR       // El proveedor reembolsa el dinero en efectivo o transferencia
}
