package com.co.jarvis.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EUserRol {
    ROLE_ADMIN, ROLE_SELLER, ROLE_CASH, ROLE_VENDEDOR, ROLE_FACTURADOR;

    @JsonCreator
    public static EUserRol fromValue(String value) {
        if (value == null) return null;
        String normalized = value.startsWith("ROLE_") ? value : "ROLE_" + value;
        return EUserRol.valueOf(normalized);
    }
}
