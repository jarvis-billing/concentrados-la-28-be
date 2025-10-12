# Configuración de Zona Horaria GMT-5 (America/Bogota)

## Problema Original
El backend normalizaba las fechas a UTC (+00:00) al persistir y devolver `dateTimeRecord`, perdiendo la hora local GMT-5.

**Ejemplo del problema:**
- Frontend envía: `2025-10-10T18:12:10-05:00`
- Backend guardaba y devolvía: `2025-10-10T23:12:10+00:00` ❌

## Solución Implementada

### 1. Configuración de Jackson (Serialización JSON)

**Archivo:** `MongoConfig.java`

```java
@Bean
public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
    return builder -> {
        // Establecer zona horaria por defecto a America/Bogota
        builder.timeZone(TimeZone.getTimeZone(BOGOTA_ZONE));
        
        // Deshabilitar timestamps numéricos, usar formato ISO-8601
        builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Configurar formato de fecha/hora para preservar offset
        builder.simpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    };
}
```

**Qué hace:**
- ✅ Establece `America/Bogota` como zona horaria por defecto
- ✅ Deshabilita timestamps numéricos (evita epoch milliseconds)
- ✅ Usa formato ISO-8601 con offset: `yyyy-MM-dd'T'HH:mm:ssXXX`
- ✅ Las respuestas JSON incluyen `-05:00`

### 2. Convertidores MongoDB

**Conversión al leer de MongoDB:**
```java
static class DateToOffsetDateTimeConverter implements Converter<Date, OffsetDateTime> {
    @Override
    public OffsetDateTime convert(@NonNull Date source) {
        // Convertir el Date (UTC) a la zona horaria de Bogota manteniendo el offset -05:00
        return source.toInstant()
                .atZone(BOGOTA_ZONE)
                .toOffsetDateTime();
    }
}
```

**Conversión al guardar en MongoDB:**
```java
static class OffsetDateTimeToDateConverter implements Converter<OffsetDateTime, Date> {
    @Override
    public Date convert(@NonNull OffsetDateTime source) {
        // Convertir a Instant (UTC) para almacenar en MongoDB
        // El offset se preserva al leer gracias a DateToOffsetDateTimeConverter
        return Date.from(source.toInstant());
    }
}
```

**Cómo funciona:**
1. **Al guardar:** `OffsetDateTime` → `Instant` (UTC) → `Date` (MongoDB)
2. **Al leer:** `Date` (MongoDB) → `Instant` → `ZonedDateTime` (Bogota) → `OffsetDateTime` (-05:00)

## Flujo Completo

### Escenario: Crear una factura

1. **Frontend envía:**
   ```json
   {
     "dateTimeRecord": "2025-10-10T18:12:10-05:00"
   }
   ```

2. **Backend recibe (Jackson deserializa):**
   ```java
   OffsetDateTime dateTimeRecord = OffsetDateTime.parse("2025-10-10T18:12:10-05:00");
   // Valor: 2025-10-10T18:12:10-05:00
   ```

3. **MongoDB guarda (Convertidor):**
   ```java
   Date mongoDate = Date.from(dateTimeRecord.toInstant());
   // MongoDB almacena: 2025-10-10T23:12:10.000Z (UTC internamente)
   ```

4. **MongoDB lee (Convertidor):**
   ```java
   OffsetDateTime result = mongoDate.toInstant()
       .atZone(ZoneId.of("America/Bogota"))
       .toOffsetDateTime();
   // Valor: 2025-10-10T18:12:10-05:00
   ```

5. **Backend responde (Jackson serializa):**
   ```json
   {
     "dateTimeRecord": "2025-10-10T18:12:10-05:00"
   }
   ```

## Criterios de Aceptación ✅

- [x] Cuando el FE envía `2025-10-10T18:12:10-05:00`, el backend guarda y devuelve exactamente ese offset y hora
- [x] No se convierte a `23:12:10+00:00`
- [x] La API de consulta retorna `dateTimeRecord` con `-05:00`
- [x] El formato es ISO-8601: `yyyy-MM-dd'T'HH:mm:ssXXX`

## Ejemplos de Uso

### Crear Factura
**Request:**
```json
POST /api/sales
{
  "dateTimeRecord": "2025-10-10T18:12:10-05:00",
  "billNumber": "FAC-001",
  ...
}
```

**Response:**
```json
{
  "id": "67890",
  "dateTimeRecord": "2025-10-10T18:12:10-05:00",
  "billNumber": "FAC-001",
  ...
}
```

### Consultar Factura
**Request:**
```
GET /api/sales/67890
```

**Response:**
```json
{
  "id": "67890",
  "dateTimeRecord": "2025-10-10T18:12:10-05:00",
  "billNumber": "FAC-001",
  ...
}
```

### Filtrar por Rango de Fechas
**Request:**
```json
POST /api/sales/filter
{
  "fromDate": "2025-10-10",
  "toDate": "2025-10-11"
}
```

**Response:**
```json
[
  {
    "id": "67890",
    "dateTimeRecord": "2025-10-10T18:12:10-05:00",
    ...
  },
  {
    "id": "67891",
    "dateTimeRecord": "2025-10-10T20:30:45-05:00",
    ...
  }
]
```

## Notas Técnicas

### MongoDB y Zonas Horarias
- MongoDB **siempre** almacena fechas como `Date` en UTC internamente
- No se puede cambiar este comportamiento
- La solución es convertir al leer/escribir usando los convertidores personalizados

### OffsetDateTime vs ZonedDateTime
- **OffsetDateTime**: Almacena fecha/hora + offset fijo (ej: `-05:00`)
- **ZonedDateTime**: Almacena fecha/hora + zona horaria (ej: `America/Bogota`)
- Usamos `OffsetDateTime` porque:
  - Es más simple y directo
  - El offset `-05:00` es constante para Colombia
  - Mejor soporte en Jackson para serialización JSON

### Horario de Verano (DST)
Colombia **NO** usa horario de verano, por lo que el offset `-05:00` es constante todo el año.

## Verificación

### 1. Verificar Serialización JSON
```bash
curl -X POST http://localhost:8080/api/sales \
  -H "Content-Type: application/json" \
  -d '{
    "dateTimeRecord": "2025-10-10T18:12:10-05:00",
    ...
  }'
```

Verificar que la respuesta incluya:
```json
"dateTimeRecord": "2025-10-10T18:12:10-05:00"
```

### 2. Verificar MongoDB
Conectarse a MongoDB y consultar:
```javascript
db.SALES_BILLING.findOne({ billNumber: "FAC-001" })
```

Verás:
```javascript
{
  "dateTimeRecord": ISODate("2025-10-10T23:12:10.000Z"), // UTC
  ...
}
```

Esto es **correcto**. MongoDB almacena en UTC, pero nuestros convertidores lo transforman a GMT-5 al leer.

### 3. Verificar Logs
Activar logs de MongoDB en `application.properties`:
```properties
logging.level.org.springframework.data.mongodb.core.MongoTemplate=DEBUG
```

Buscar en logs:
```
Saving entity: dateTimeRecord=2025-10-10T18:12:10-05:00
```

## Troubleshooting

### Problema: Sigue devolviendo UTC
**Causa:** Jackson no está usando la configuración personalizada.

**Solución:** Verificar que `MongoConfig` tenga `@Configuration` y el bean `jsonCustomizer()`.

### Problema: Error de deserialización
**Causa:** El frontend envía formato incorrecto.

**Solución:** Verificar que el frontend use:
```javascript
formatInTimeZone(new Date(), 'America/Bogota', "yyyy-MM-dd'T'HH:mm:ssXXX")
```

### Problema: Offset incorrecto en consultas
**Causa:** Los filtros de fecha no están convirtiendo correctamente.

**Solución:** Verificar que `SaleServiceImpl` use:
```java
dto.getFromDate().atStartOfDay().atZone(ZoneId.of("America/Bogota")).toOffsetDateTime()
```

## Resumen

✅ **Configuración completada:**
- Jackson configurado para preservar offset `-05:00`
- Convertidores MongoDB para transformar UTC ↔ GMT-5
- Zona horaria `America/Bogota` establecida por defecto
- Formato ISO-8601 con offset en todas las respuestas

✅ **Resultado:**
- Frontend envía: `2025-10-10T18:12:10-05:00`
- Backend guarda: `2025-10-10T23:12:10.000Z` (UTC en MongoDB)
- Backend devuelve: `2025-10-10T18:12:10-05:00` ✅

La hora local GMT-5 se mantiene intacta en toda la aplicación.
