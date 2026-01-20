package com.co.jarvis.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class DateTimeUtil {

    private static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");
    private static final ZoneOffset BOGOTA_OFFSET = ZoneOffset.ofHours(-5);

    private DateTimeUtil() {
    }

    public static LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now(BOGOTA_ZONE);
    }

    public static OffsetDateTime nowOffsetDateTime() {
        return LocalDateTime.now(BOGOTA_ZONE).atOffset(BOGOTA_OFFSET);
    }

    public static ZoneId getBogotaZone() {
        return BOGOTA_ZONE;
    }

    public static ZoneOffset getBogotaOffset() {
        return BOGOTA_OFFSET;
    }

    public static OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(BOGOTA_ZONE).toOffsetDateTime();
    }

    public static LocalDateTime toLocalDateTime(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        return offsetDateTime.atZoneSameInstant(BOGOTA_ZONE).toLocalDateTime();
    }
}
