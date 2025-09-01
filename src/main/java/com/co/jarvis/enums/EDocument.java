package com.co.jarvis.enums;

import com.co.jarvis.util.mensajes.MessageConstants;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum EDocument {
    CEDULA_CIUDADANIA("CC"),
    NIT("NIT"),
    PASAPORTE("PA"),
    CEDULA_EXTRANJERIA("CE");

    private String value;

    EDocument(String value) {
        this.value = value;
    }

    public static EDocument getByValue(String value) {
        return Arrays.stream(EDocument.values())
                .filter(document -> document.getValue().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(MessageConstants.RESOURCE_NOT_FOUND));
    }
}
