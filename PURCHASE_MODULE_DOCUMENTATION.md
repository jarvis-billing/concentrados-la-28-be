# Módulo de Compras - Documentación

## Descripción General

Este módulo implementa la funcionalidad completa de **facturas de compra** (Purchase Invoices) para el sistema de inventario, incluyendo la **actualización automática del stock de productos** al crear o modificar facturas de compra.

## Stack Tecnológico

- **Java 17**
- **Spring Boot 3.2.2**
- **MongoDB** con Spring Data MongoDB
- **Lombok** para reducir boilerplate
- **Maven** como gestor de dependencias

## Arquitectura

El módulo sigue la arquitectura en capas del proyecto:

```
├── entity/              # Entidades de MongoDB
├── dto/                 # Data Transfer Objects
├── enums/              # Enumeraciones
├── repository/         # Interfaces de acceso a datos
├── service/            # Lógica de negocio
│   └── impl/          # Implementaciones
└── controller/         # Endpoints REST
```

## Componentes Creados

### 1. Entidades (entity/)

#### `PurchaseInvoice`
- **Colección MongoDB**: `PURCHASE_INVOICES`
- **Campos principales**:
  - `id`: Identificador único
  - `invoiceNumber`: Número de factura
  - `supplierId`, `supplierName`: Datos del proveedor
  - `date`: Fecha de la compra
  - `items`: Lista de items de la factura
  - `totalAmount`: Monto total
  - `status`: Estado (CREATED, CANCELLED)
  - `creationUser`: Usuario que creó la factura
  - `createdAt`, `updatedAt`: Timestamps

#### `PurchaseInvoiceItem`
- **Campos**:
  - `productId`, `productCode`: Referencia al producto
  - `description`: Descripción del producto
  - `quantity`: Cantidad comprada
  - `unitCost`: Costo unitario
  - `totalCost`: Costo total del item

### 2. Enumeraciones (enums/)

#### `EPurchaseInvoiceStatus`
- `CREATED`: Factura creada
- `CANCELLED`: Factura cancelada

### 3. DTOs (dto/)

- `PurchaseInvoiceDto`: DTO para factura de compra completa
- `PurchaseInvoiceItemDto`: DTO para items de factura
- `PurchaseFilterDto`: DTO para filtros de búsqueda

### 4. Repository (repository/)

#### `PurchaseInvoiceRepository`
Extiende `MongoRepository` con métodos personalizados:
- `findByInvoiceNumber(String invoiceNumber)`
- `findBySupplierId(String supplierId)`
- `findByDateBetween(OffsetDateTime dateFrom, OffsetDateTime dateTo)`

### 5. Service (service/)

#### `PurchaseInvoiceService` (interfaz)
Define operaciones CRUD:
- `list(PurchaseFilterDto filter)`: Listar con filtros
- `findById(String id)`: Buscar por ID
- `create(PurchaseInvoiceDto dto)`: Crear factura
- `update(String id, PurchaseInvoiceDto dto)`: Actualizar factura
- `deleteById(String id)`: Eliminar factura

#### `PurchaseInvoiceServiceImpl` (implementación)
Implementa la lógica de negocio completa, incluyendo:

**Lógica de Stock en Creación**:
1. Valida la factura (items, cantidades, costos)
2. Guarda la factura en MongoDB
3. Por cada item:
   - Busca el producto correspondiente
   - Incrementa el stock llamando a `product.increaseStock(quantity)`
   - Guarda el producto actualizado

**Lógica de Stock en Actualización**:
1. Recupera la factura original
2. Calcula diferencias entre versión original y nueva:
   - Si un producto aumentó cantidad → incrementa stock con la diferencia
   - Si un producto redujo cantidad → reduce stock con la diferencia
   - Si se agregó un producto nuevo → incrementa stock con toda la cantidad
   - Si se quitó un producto → reduce stock con toda la cantidad
3. Aplica los ajustes y guarda

**Transaccionalidad**:
- Métodos `create` y `update` están anotados con `@Transactional`
- Garantiza consistencia entre factura y stock de productos

### 6. Controller (controller/)

#### `PurchaseInvoiceController`
Expone los siguientes endpoints REST:

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/purchases/invoices` | Lista facturas con filtros opcionales |
| GET | `/api/purchases/invoices/{id}` | Obtiene factura por ID |
| POST | `/api/purchases/invoices` | Crea factura (actualiza stock) |
| PUT | `/api/purchases/invoices/{id}` | Actualiza factura (ajusta stock) |
| DELETE | `/api/purchases/invoices/{id}` | Elimina factura |

**Parámetros de filtro** (GET):
- `dateFrom`, `dateTo`: Rango de fechas
- `supplierId`: ID del proveedor
- `supplierName`: Nombre del proveedor
- `invoiceNumber`: Número de factura

**Respuestas HTTP**:
- `200 OK`: Operación exitosa
- `201 CREATED`: Factura creada
- `204 NO CONTENT`: Eliminación exitosa
- `400 BAD REQUEST`: Validación fallida
- `404 NOT FOUND`: Recurso no encontrado
- `500 INTERNAL SERVER ERROR`: Error del servidor

## Reglas de Negocio

### 1. Validaciones
- Una factura debe tener al menos un item
- Cada item debe tener `productId` o `productCode`
- La cantidad debe ser mayor a cero
- El costo unitario no puede ser negativo

### 2. Actualización de Stock

#### Al Crear Factura (POST)
```
Por cada item en la factura:
  1. Buscar producto (por productId o productCode)
  2. Incrementar stock: stock.quantity += item.quantity
  3. Guardar producto actualizado
```

#### Al Actualizar Factura (PUT)
```
Por cada producto afectado:
  1. Calcular: diferencia = nueva_cantidad - cantidad_original
  2. Si diferencia > 0: incrementar stock
  3. Si diferencia < 0: reducir stock
  4. Guardar producto actualizado
```

### 3. Cálculo de Totales
- `totalCost` de item = `quantity * unitCost`
- `totalAmount` de factura = suma de todos los `totalCost`

## Integración con Frontend Angular

El frontend debe usar el servicio Angular que llama a estos endpoints:

```typescript
@Injectable({ providedIn: 'root' })
export class PurchasesService {
  private baseUrl = '/api/purchases/invoices';

  list(params?: any): Observable<PurchaseInvoice[]> {
    return this.http.get<PurchaseInvoice[]>(this.baseUrl, { params });
  }

  create(payload: PurchaseInvoice): Observable<PurchaseInvoice> {
    return this.http.post<PurchaseInvoice>(this.baseUrl, payload);
  }

  update(id: string, payload: PurchaseInvoice): Observable<PurchaseInvoice> {
    return this.http.put<PurchaseInvoice>(`${this.baseUrl}/${id}`, payload);
  }
}
```

## Manejo de Errores

El módulo utiliza el `GlobalExceptionHandler` existente del proyecto para manejar:
- `ResourceNotFoundException`: Producto o factura no encontrado
- `SaveRecordException`: Error al guardar o validar
- `DeleteRecordException`: Error al eliminar
- `MethodArgumentNotValidException`: Validación de campos

Las respuestas de error siguen el formato estándar del proyecto.

## Consideraciones Importantes

### Transacciones MongoDB
- Los métodos `create` y `update` usan `@Transactional`
- MongoDB debe estar configurado con replica sets para soportar transacciones
- Si no hay replica sets, las operaciones se ejecutan secuencialmente sin rollback automático

### Consistencia de Datos
- El sistema asume que los productos existen antes de crear facturas
- Si un producto no existe, se lanza `ResourceNotFoundException`
- El stock se actualiza inmediatamente al guardar la factura

### Eliminación de Facturas
- El endpoint DELETE actualmente **no revierte el stock**
- Considerar implementar lógica adicional si se requiere revertir stock al eliminar

## Pruebas Recomendadas

1. **Crear factura de compra**
   - Verificar que se guarda correctamente
   - Verificar que el stock de productos aumenta

2. **Actualizar factura aumentando cantidades**
   - Verificar que el stock aumenta la diferencia

3. **Actualizar factura reduciendo cantidades**
   - Verificar que el stock disminuye la diferencia

4. **Actualizar factura agregando productos**
   - Verificar que el stock del nuevo producto aumenta

5. **Actualizar factura eliminando productos**
   - Verificar que el stock del producto eliminado disminuye

6. **Validaciones**
   - Intentar crear factura sin items
   - Intentar crear item con cantidad cero o negativa
   - Intentar crear item con producto inexistente

## Ejemplo de Uso

### Crear Factura de Compra

**Request POST** `/api/purchases/invoices`:
```json
{
  "invoiceNumber": "COMP-001",
  "supplierId": "supplier123",
  "supplierName": "Proveedor XYZ",
  "date": "2024-11-26T10:30:00-05:00",
  "items": [
    {
      "productId": "prod001",
      "description": "Producto A",
      "quantity": 100,
      "unitCost": 15.50,
      "totalCost": 1550.00
    },
    {
      "productCode": "P002",
      "description": "Producto B",
      "quantity": 50,
      "unitCost": 20.00,
      "totalCost": 1000.00
    }
  ],
  "totalAmount": 2550.00
}
```

**Response 201 Created**:
```json
{
  "id": "673e8f1a2b3c4d5e6f7a8b9c",
  "invoiceNumber": "COMP-001",
  "supplierId": "supplier123",
  "supplierName": "Proveedor XYZ",
  "date": "2024-11-26T10:30:00-05:00",
  "items": [...],
  "totalAmount": 2550.00,
  "status": "CREATED",
  "createdAt": "2024-11-26T10:35:00-05:00",
  "updatedAt": "2024-11-26T10:35:00-05:00"
}
```

**Efecto en Stock**:
- Producto con ID `prod001`: stock += 100
- Producto con código `P002`: stock += 50

## Archivos Creados

```
src/main/java/com/co/jarvis/
├── entity/
│   ├── PurchaseInvoice.java
│   └── PurchaseInvoiceItem.java
├── dto/
│   ├── PurchaseInvoiceDto.java
│   ├── PurchaseInvoiceItemDto.java
│   └── PurchaseFilterDto.java
├── enums/
│   └── EPurchaseInvoiceStatus.java
├── repository/
│   └── PurchaseInvoiceRepository.java
├── service/
│   ├── PurchaseInvoiceService.java
│   └── impl/
│       └── PurchaseInvoiceServiceImpl.java
└── controller/
    └── PurchaseInvoiceController.java
```

## Próximos Pasos Sugeridos

1. **Configurar seguridad**: Agregar autenticación/autorización a los endpoints
2. **Tests unitarios**: Implementar tests para la lógica de stock
3. **Tests de integración**: Verificar el flujo completo con MongoDB
4. **Auditoría**: Considerar agregar logs de auditoría para cambios de stock
5. **Reportes**: Implementar reportes de compras por período
6. **Validaciones adicionales**: Validar stock suficiente al actualizar (si se reduce)
7. **Manejo de eliminación**: Implementar lógica para revertir stock al eliminar factura

## Soporte

Para dudas o problemas con el módulo, revisar:
- Logs de aplicación: búsqueda por `PurchaseInvoiceServiceImpl`
- Validar configuración de MongoDB (replica sets para transacciones)
- Verificar que los productos existen antes de crear facturas
