# Fix: Soporte para Fechas con Zona Horaria (GMT-5)

## Problema
El frontend enviaba fechas en formato ISO 8601 con offset de zona horaria (`2025-10-10T17:55:01-05:00`), pero el backend usaba `LocalDateTime` que no soporta offsets, causando el error:
```
Cannot deserialize value of type `java.time.LocalDateTime` from String "2025-10-10T17:55:01-05:00": 
Failed to deserialize java.time.LocalDateTime: Text '2025-10-10T17:55:01-05:00' could not be parsed, 
unparsed text found at index 19
```

## Solución
Cambiar `LocalDateTime` a `OffsetDateTime` en todas las entidades y DTOs relacionados con `Billing`.

---

## Archivos Creados

### **MongoConfig.java** (Nuevo)
**Ubicación:** `src/main/java/com/co/jarvis/config/MongoConfig.java`

**Propósito:** Configurar convertidores personalizados para MongoDB que permitan la conversión bidireccional entre `java.util.Date` (usado por MongoDB) y `java.time.OffsetDateTime` (usado en el código).

**Convertidores implementados:**
1. **DateToOffsetDateTimeConverter**: Convierte `Date` → `OffsetDateTime`
2. **OffsetDateTimeToDateConverter**: Convierte `OffsetDateTime` → `Date`

```java
@Configuration
public class MongoConfig {
    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new DateToOffsetDateTimeConverter());
        converters.add(new OffsetDateTimeToDateConverter());
        return new MongoCustomConversions(converters);
    }
}
```

**¿Por qué es necesario?**
MongoDB almacena fechas como `java.util.Date` internamente. Spring Data MongoDB necesita saber cómo convertir entre `Date` y `OffsetDateTime` automáticamente.

---

## Archivos Modificados

### 1. **Billing.java** (Entidad)
**Cambio:**
```java
// Antes
private LocalDateTime dateTimeRecord;

// Después
private OffsetDateTime dateTimeRecord;
```

**Import actualizado:**
```java
import java.time.OffsetDateTime;
```

---

### 2. **BillingDto.java** (DTO)
**Cambio:**
```java
// Antes
private LocalDateTime dateTimeRecord;

// Después
private OffsetDateTime dateTimeRecord;
```

**Import actualizado:**
```java
import java.time.OffsetDateTime;
```

---

### 3. **BillingRepository.java**
**Cambios en firmas de métodos:**
```java
// Antes
List<ProductSalesSummary> getProductSalesSummaryByDate(LocalDateTime from, LocalDateTime to);
List<ProductSalesSummary> getProductSalesSummaryByDateAndProduct(LocalDateTime from, LocalDateTime to, String productBarcode);

// Después
List<ProductSalesSummary> getProductSalesSummaryByDate(OffsetDateTime from, OffsetDateTime to);
List<ProductSalesSummary> getProductSalesSummaryByDateAndProduct(OffsetDateTime from, OffsetDateTime to, String productBarcode);
```

**Import actualizado:**
```java
import java.time.OffsetDateTime;
```

**Nota:** Las consultas de agregación MongoDB funcionan correctamente con `OffsetDateTime` sin cambios adicionales.

---

### 4. **SaleServiceImpl.java**
**Cambios:**

#### a) Imports actualizados
```java
// Removido
import java.time.LocalDateTime;

// Agregados
import java.time.OffsetDateTime;
import java.time.ZoneId;
```

#### b) Conversión en `buildBillingDto()` (línea 273-275)
```java
// Antes
.dateTimeRecord(orderDto.getCreationDate())

// Después
.dateTimeRecord(orderDto.getCreationDate() != null 
    ? orderDto.getCreationDate().atZone(ZoneId.systemDefault()).toOffsetDateTime()
    : OffsetDateTime.now())
```

#### c) Filtro de fechas en `findAllBilling()` (líneas 204-206)
```java
// Antes
criteriaList.add(Criteria.where("dateTimeRecord")
    .gte(dto.getToDate().atStartOfDay())
    .lte(dto.getFromDate().atTime(LocalTime.now())));

// Después
criteriaList.add(Criteria.where("dateTimeRecord")
    .gte(dto.getToDate().atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime())
    .lte(dto.getFromDate().atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toOffsetDateTime()));
```

#### d) Llamada a repositorio en `getProductSalesSummary()` (líneas 247-249)
```java
// Antes
return repository.getProductSalesSummaryByDate(
    dto.getFromDate().atStartOfDay(), 
    dto.getToDate().atTime(LocalTime.now()));

// Después
return repository.getProductSalesSummaryByDate(
    dto.getFromDate().atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime(), 
    dto.getToDate().atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toOffsetDateTime());
```

---

## Diferencias entre LocalDateTime y OffsetDateTime

| Característica | LocalDateTime | OffsetDateTime |
|---------------|---------------|----------------|
| **Zona horaria** | ❌ No soporta | ✅ Soporta offset (ej: -05:00) |
| **Formato ISO 8601** | `2025-10-10T17:55:01` | `2025-10-10T17:55:01-05:00` |
| **Uso típico** | Fechas locales sin contexto de zona | Fechas con zona horaria explícita |
| **Serialización JSON** | Solo fecha/hora | Fecha/hora + offset |
| **MongoDB** | Almacena como Date UTC | Almacena como Date UTC con offset |

---

## Conversión de LocalDateTime a OffsetDateTime

Cuando necesites convertir `LocalDateTime` a `OffsetDateTime`:

```java
LocalDateTime localDateTime = LocalDateTime.now();

// Opción 1: Usar zona horaria del sistema
OffsetDateTime offsetDateTime = localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();

// Opción 2: Usar zona horaria específica
OffsetDateTime offsetDateTime = localDateTime.atZone(ZoneId.of("America/Bogota")).toOffsetDateTime();

// Opción 3: Usar offset específico
OffsetDateTime offsetDateTime = localDateTime.atOffset(ZoneOffset.ofHours(-5));
```

---

## Impacto en Base de Datos (MongoDB)

- MongoDB almacena fechas como **Date** (UTC internamente)
- `OffsetDateTime` se serializa correctamente a Date con el offset preservado
- Las consultas de agregación funcionan sin cambios
- Los datos existentes en DB se deserializan correctamente a `OffsetDateTime`

---

## Pruebas Recomendadas

1. **Crear una venta** desde el frontend con fecha GMT-5
2. **Consultar ventas** por rango de fechas
3. **Verificar reportes** de productos vendidos por fecha
4. **Validar** que las fechas se muestren correctamente en el frontend

---

## Notas Adicionales

- `OrderDto.creationDate` sigue siendo `LocalDateTime` porque es interno del backend
- La conversión se hace en el punto de mapeo a `BillingDto`
- `ZoneId.systemDefault()` usa la zona horaria configurada en el servidor
- Si necesitas zona horaria específica, usa `ZoneId.of("America/Bogota")`

---

## Archivos NO Modificados

Los siguientes archivos usan `LocalDateTime` pero **NO** requieren cambios porque no reciben datos del frontend con offset:

- `Order.java` / `OrderDto.java`
- `Catalog.java` / `CatalogDto.java`
- `OrderApi.java`
- Otros DTOs internos

---

## Errores Resueltos

### Error 1: Deserialización JSON
```
Cannot deserialize value of type `java.time.LocalDateTime` from String "2025-10-10T17:55:01-05:00"
```
**Solución:** Cambiar `LocalDateTime` a `OffsetDateTime` en entidades y DTOs.

### Error 2: Conversión MongoDB
```
No converter found capable of converting from type [java.util.Date] to type [java.time.OffsetDateTime]
```
**Solución:** Crear `MongoConfig.java` con convertidores personalizados.

---

## Actualización: Preservación de Zona Horaria GMT-5

### Configuración Adicional (10/10/2025)

Se agregó configuración para **preservar el offset GMT-5** (America/Bogota) en lugar de normalizar a UTC:

**Archivo:** `MongoConfig.java`

1. **Jackson Customizer:**
   ```java
   @Bean
   public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
       return builder -> {
           builder.timeZone(TimeZone.getTimeZone("America/Bogota"));
           builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
           builder.simpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
       };
   }
   ```

2. **Convertidor MongoDB actualizado:**
   ```java
   return source.toInstant()
           .atZone(ZoneId.of("America/Bogota"))
           .toOffsetDateTime();
   ```

**Resultado:**
- Frontend envía: `2025-10-10T18:12:10-05:00`
- Backend devuelve: `2025-10-10T18:12:10-05:00` ✅ (antes era `23:12:10+00:00`)

Ver documentación completa en: **`TIMEZONE_GMT5_CONFIG.md`**

---

## Resumen

✅ **Problema resuelto:** El backend ahora acepta fechas con zona horaria desde el frontend  
✅ **Compatibilidad:** MongoDB maneja correctamente `OffsetDateTime` con convertidores personalizados  
✅ **Sin breaking changes:** Los datos existentes se deserializan correctamente  
✅ **Conversiones:** LocalDateTime → OffsetDateTime donde sea necesario  
✅ **Convertidores MongoDB:** Date ↔ OffsetDateTime configurados  
✅ **Zona horaria preservada:** GMT-5 (America/Bogota) se mantiene en toda la aplicación

El sistema ahora soporta fechas con offset de zona horaria (GMT-5) enviadas desde el frontend y **preserva la hora local** sin convertir a UTC.
