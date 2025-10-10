# Implementación de Display Stock (Stock en Bultos/Rollos)

## Resumen
Se implementó el cálculo y exposición de "stock legible" en packs (bultos/rollos) + sobrante, manteniendo el stock real en la unidad base.

## Archivos Creados

### 1. `DisplayStock.java` (DTO)
Ubicación: `src/main/java/com/co/jarvis/dto/DisplayStock.java`

Contrato API con los siguientes campos:
- `kind`: 'WEIGHT' | 'LONGITUDE' | null
- `packSize`: Tamaño del bulto/rollo en unidad base (BigDecimal)
- `packs`: Cantidad de paquetes completos (Integer)
- `remainder`: Cantidad sobrante en unidad base (BigDecimal)
- `unit`: Unidad base ('kg', 'cm', etc.)
- `label`: Texto legible (ej: "12 bultos + 8 kg")
- `computedAt`: Timestamp ISO 8601

### 2. `UnitConverter.java` (Utilidad)
Ubicación: `src/main/java/com/co/jarvis/util/UnitConverter.java`

Helper para conversión de unidades:
- `toBase(value, fromUnit, baseUnit)`: Convierte valores a unidad base
- `round(value, decimals)`: Redondea BigDecimal
- `isEffectivelyZero(value)`: Verifica si un valor es efectivamente cero

Conversiones soportadas:
- **WEIGHT**: base = kg
  - KILOGRAMOS → kg (1:1)
- **LONGITUDE**: base = cm
  - CENTIMETROS → cm (1:1)
  - METROS → cm (1:100)
- **VOLUME**: base = L
  - LITROS → L (1:1)
  - MILILITROS → L (1:1000)

### 3. Actualización de `UnitMeasure.java`
Se agregó el enum `METROS` para soportar conversiones de longitud.

## Cambios en Servicios

### `ProductService.java`
Se agregó el método:
```java
DisplayStock computeDisplayStock(Product product);
```

### `ProductServiceImpl.java`
Se implementó:
1. **`computeDisplayStock(Product)`**: Calcula el displayStock según las reglas de negocio
2. **`enrichProductDto(Product)`**: Helper que mapea Product → ProductDto y agrega displayStock
3. **Métodos helper**:
   - `determineKind(ESale)`: Determina el tipo (WEIGHT/LONGITUDE)
   - `determineBaseUnit(String, UnitMeasure)`: Determina la unidad base
   - `findLargestPackSize(Product, String)`: Encuentra el mayor fixedAmount válido
   - `buildEmptyDisplayStock()`: Construye displayStock vacío

### Endpoints actualizados
Todos los endpoints que retornan productos ahora incluyen `displayStock`:
- `GET /products` (findAll)
- `GET /products/{id}` (findById)
- `GET /products/page` (findAllPage)
- `GET /products/search` (findAllPageSearch)
- `GET /products/barcode/{barcode}` (findByPresentationsBarcode)
- `POST /products` (save)
- `PUT /products/{barcode}` (update)

## Reglas de Cómputo

### Determinación de `packSize`
1. Filtrar presentaciones con `isFixedAmount = true`
2. Filtrar presentaciones con `fixedAmount > 0`
3. Convertir todos los `fixedAmount` a la unidad base
4. Tomar el **MAYOR** como `packSize`

### Cálculo de packs y remainder
```java
packs = floor(stock.quantity / packSize)
remainder = stock.quantity - (packs * packSize)
```

### Generación de label
- **WEIGHT**: `"{packs} bultos + {remainder} kg"`
- **LONGITUDE**: `"{packs} rollos + {remainder} cm"`
- **Sin packSize**: `"{quantity} {unit}"`

## Casos de Prueba

### Caso 1: WEIGHT con presentaciones fijas
**Input:**
```json
{
  "saleType": "WEIGHT",
  "stock": {
    "quantity": 248,
    "unitMeasure": "KILOGRAMOS"
  },
  "presentations": [
    {
      "isFixedAmount": true,
      "fixedAmount": 20,
      "unitMeasure": "KILOGRAMOS"
    }
  ]
}
```

**Output esperado:**
```json
{
  "displayStock": {
    "kind": "WEIGHT",
    "packSize": 20,
    "packs": 12,
    "remainder": 8,
    "unit": "kg",
    "label": "12 bultos + 8 kg",
    "computedAt": "2025-10-09T08:55:00Z"
  }
}
```

### Caso 2: WEIGHT sin presentaciones fijas
**Input:**
```json
{
  "saleType": "WEIGHT",
  "stock": {
    "quantity": 248,
    "unitMeasure": "KILOGRAMOS"
  },
  "presentations": []
}
```

**Output esperado:**
```json
{
  "displayStock": {
    "kind": "WEIGHT",
    "packSize": null,
    "packs": null,
    "remainder": null,
    "unit": "kg",
    "label": "248 kg",
    "computedAt": "2025-10-09T08:55:00Z"
  }
}
```

### Caso 3: LONGITUDE con conversión de unidades
**Input:**
```json
{
  "saleType": "LONGITUDE",
  "stock": {
    "quantity": 78,
    "unitMeasure": "METROS"
  },
  "presentations": [
    {
      "isFixedAmount": true,
      "fixedAmount": 50,
      "unitMeasure": "METROS"
    }
  ]
}
```

**Output esperado:**
```json
{
  "displayStock": {
    "kind": "LONGITUDE",
    "packSize": 5000,
    "packs": 1,
    "remainder": 2800,
    "unit": "cm",
    "label": "1 rollos + 2800 cm",
    "computedAt": "2025-10-09T08:55:00Z"
  }
}
```

### Caso 4: Múltiples presentaciones fijas (usa la mayor)
**Input:**
```json
{
  "saleType": "WEIGHT",
  "stock": {
    "quantity": 100,
    "unitMeasure": "KILOGRAMOS"
  },
  "presentations": [
    {
      "isFixedAmount": true,
      "fixedAmount": 10,
      "unitMeasure": "KILOGRAMOS"
    },
    {
      "isFixedAmount": true,
      "fixedAmount": 20,
      "unitMeasure": "KILOGRAMOS"
    }
  ]
}
```

**Output esperado:**
```json
{
  "displayStock": {
    "kind": "WEIGHT",
    "packSize": 20,
    "packs": 5,
    "remainder": 0,
    "unit": "kg",
    "label": "5 bultos + 0 kg",
    "computedAt": "2025-10-09T08:55:00Z"
  }
}
```

### Caso 5: Producto tipo UNIT (no aplica bultos)
**Input:**
```json
{
  "saleType": "UNIT",
  "stock": {
    "quantity": 150,
    "unitMeasure": "UNIDAD"
  },
  "presentations": []
}
```

**Output esperado:**
```json
{
  "displayStock": {
    "kind": null,
    "packSize": null,
    "packs": null,
    "remainder": null,
    "unit": "Unit",
    "label": "150 Unit",
    "computedAt": "2025-10-09T08:55:00Z"
  }
}
```

## Edge Cases Manejados

1. **Stock nulo o negativo**: Se trata como 0
2. **fixedAmount flotante**: Se usa tolerancia para comparaciones
3. **Múltiples presentaciones fijas**: Se usa el MAYOR packSize
4. **Redondeo de remainder**: Se redondea a 3 decimales
5. **Unidades mezcladas**: Se convierten correctamente a la unidad base antes de calcular
6. **Producto sin stock**: Retorna displayStock vacío con label "0"

## Notas de Implementación

- El cálculo de `displayStock` se realiza **en tiempo real** cada vez que se consulta un producto
- No se persiste en la base de datos (es un campo calculado)
- La conversión de unidades es extensible para agregar más tipos en `UnitConverter`
- El timestamp `computedAt` permite al frontend saber cuándo se calculó el stock

## Próximos Pasos Sugeridos

1. **Testing**: Crear tests unitarios para `computeDisplayStock` y `UnitConverter`
2. **Performance**: Si hay muchos productos, considerar cachear el cálculo
3. **Frontend**: Actualizar interfaces para mostrar el `displayStock.label`
4. **Documentación API**: Actualizar Swagger/OpenAPI con el nuevo campo
5. **Validaciones**: Agregar validaciones en el frontend para `fixedAmount` según `saleType`
