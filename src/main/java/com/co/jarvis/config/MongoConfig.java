package com.co.jarvis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.NonNull;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Configuración de MongoDB y Jackson para manejar fechas con zona horaria America/Bogota (GMT-5)
 * Preserva el offset -05:00 al guardar y devolver dateTimeRecord
 */
@Configuration
public class MongoConfig {

    private static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");

    /**
     * Configura Jackson para preservar el offset -05:00 en la serialización JSON
     * Esto asegura que las respuestas JSON incluyan el offset: "2025-10-10T18:12:10-05:00"
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            // Establecer zona horaria por defecto a America/Bogota
            builder.timeZone(TimeZone.getTimeZone(BOGOTA_ZONE));
            
            // Deshabilitar timestamps numéricos, usar formato ISO-8601
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            
            // Habilitar módulo de Java Time para OffsetDateTime
            builder.modules(new JavaTimeModule());
        };
    }

    /**
     * Bean alternativo de ObjectMapper con configuración explícita
     * Usar este si jsonCustomizer() no funciona
     */
    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.build();
        mapper.setTimeZone(TimeZone.getTimeZone(BOGOTA_ZONE));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * Configura convertidores personalizados para MongoDB
     */
    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new DateToOffsetDateTimeConverter());
        converters.add(new OffsetDateTimeToDateConverter());
        return new MongoCustomConversions(converters);
    }

    /**
     * Convierte Date (MongoDB) a OffsetDateTime (Java)
     * Preserva la zona horaria America/Bogota (GMT-5)
     */
    static class DateToOffsetDateTimeConverter implements Converter<Date, OffsetDateTime> {
        @Override
        public OffsetDateTime convert(@NonNull Date source) {
            // Convertir el Date (UTC) a la zona horaria de Bogota manteniendo el offset -05:00
            return source.toInstant()
                    .atZone(BOGOTA_ZONE)
                    .toOffsetDateTime();
        }
    }

    /**
     * Convierte OffsetDateTime (Java) a Date (MongoDB)
     * MongoDB almacena en UTC pero preservamos el instant correcto
     */
    static class OffsetDateTimeToDateConverter implements Converter<OffsetDateTime, Date> {
        @Override
        public Date convert(@NonNull OffsetDateTime source) {
            // Convertir a Instant (UTC) para almacenar en MongoDB
            // El offset se preserva al leer gracias a DateToOffsetDateTimeConverter
            return Date.from(source.toInstant());
        }
    }
}
