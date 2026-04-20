package com.co.jarvis.enums;

/**
 * Estado del ciclo de vida de un traslado interno.
 * ACTIVO: El traslado es válido y afecta el saldo de la caja.
 * ANULADO: El traslado fue revertido y debe ignorarse en el arqueo.
 */
public enum EInternalTransferStatus {
    ACTIVO,
    ANULADO
}
