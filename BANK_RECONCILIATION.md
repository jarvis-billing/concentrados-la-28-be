# Arqueo Bancario — Documentación técnica

Módulo para conciliar los movimientos **no-efectivo** por cada cuenta bancaria de la empresa,
de forma independiente al Arqueo de Caja (EFECTIVO).

---

## Índice

1. [Modelo de datos](#1-modelo-de-datos)
2. [Cuentas bancarias — CRUD](#2-cuentas-bancarias--crud)
3. [Conciliación bancaria — flujo de uso](#3-conciliación-bancaria--flujo-de-uso)
4. [Referencia de endpoints](#4-referencia-de-endpoints)
5. [Lógica de cálculo](#5-lógica-de-cálculo)
6. [Limitaciones actuales y hoja de ruta](#6-limitaciones-actuales-y-hoja-de-ruta)

---

## 1. Modelo de datos

### `BANK_ACCOUNTS` — Colección MongoDB

Representa cada cuenta bancaria de la empresa.

| Campo            | Tipo        | Descripción                              |
|------------------|-------------|------------------------------------------|
| `id`             | String      | ObjectId Mongo (clave primaria)          |
| `name`           | String      | Alias descriptivo, e.g. "Bancolombia Ahorros Principal" |
| `bankName`       | String      | Nombre del banco, e.g. "Bancolombia"     |
| `accountNumber`  | String      | Número de cuenta (puede ser últimos 4 dígitos) |
| `accountType`    | EBankAccount| `AHORROS` o `CORRIENTE`                  |
| `active`         | boolean     | `true` = activa, `false` = desactivada (soft delete) |
| `notes`          | String      | Notas opcionales                         |
| `createdAt`      | LocalDateTime| Fecha de creación                       |
| `updatedAt`      | LocalDateTime| Última modificación                     |

### `BANK_RECONCILIATION_SESSIONS` — Colección MongoDB

Una sesión de conciliación por cuenta bancaria **y** día (índice único compuesto).

| Campo               | Tipo             | Descripción                                          |
|---------------------|------------------|------------------------------------------------------|
| `id`                | String           | ObjectId Mongo                                       |
| `sessionDate`       | LocalDate        | Fecha del día conciliado                             |
| `bankAccountId`     | String           | Referencia a `BANK_ACCOUNTS._id`                     |
| `bankAccountName`   | String           | Nombre desnormalizado (para mostrar sin joins)       |
| `openingBalance`    | BigDecimal       | Saldo inicial del banco (ingresado por el usuario)   |
| `totalBankCounted`  | BigDecimal       | Saldo final según extracto/app bancaria              |
| `expectedBankAmount`| BigDecimal       | Flujo neto calculado por el sistema (ingresos−egresos) |
| `expectedBankTotal` | BigDecimal       | `openingBalance + expectedBankAmount`                |
| `difference`        | BigDecimal       | `totalBankCounted − expectedBankTotal`               |
| `totalIncome`       | BigDecimal       | Suma de todos los INGRESOS no-efectivo del día       |
| `totalExpense`      | BigDecimal       | Suma de todos los EGRESOS no-efectivo del día        |
| `totalTransfers`    | BigDecimal       | Total de traslados caja→banco a esta cuenta          |
| `netBankFlow`       | BigDecimal       | `totalIncome − totalExpense`                         |
| `status`            | ECashCountStatus | `EN_PROGRESO`, `CERRADO`, `ANULADO`                  |
| `notes`             | String           | Notas del usuario                                    |
| `cancelReason`      | String           | Motivo de anulación (si aplica)                      |
| `auditTrail`        | List\<AuditEntry>| Historial de acciones con usuario, acción y timestamp|

**Índice único compuesto:** `{ sessionDate: 1, bankAccountId: 1 }` — una sola conciliación por cuenta por día.

---

## 2. Cuentas bancarias — CRUD

Base URL: `/api/bank-accounts`

### Crear una cuenta

```http
POST /api/bank-accounts
Content-Type: application/json

{
  "name": "Bancolombia Ahorros Principal",
  "bankName": "Bancolombia",
  "accountNumber": "****1234",
  "accountType": "AHORROS",
  "notes": "Cuenta principal para recaudo de transferencias"
}
```

**Respuesta `200 OK`:**
```json
{
  "id": "664a1b2c3d4e5f6a7b8c9d0e",
  "name": "Bancolombia Ahorros Principal",
  "bankName": "Bancolombia",
  "accountNumber": "****1234",
  "accountType": "AHORROS",
  "active": true,
  "notes": "Cuenta principal para recaudo de transferencias",
  "createdAt": "2026-04-26T08:00:00",
  "updatedAt": "2026-04-26T08:00:00"
}
```

> ⚠️ **Guarda el `id`** — lo necesitarás en todos los endpoints de conciliación.

### Listar cuentas activas

```http
GET /api/bank-accounts
```

### Listar todas (incluye inactivas)

```http
GET /api/bank-accounts/all
```

### Obtener por ID

```http
GET /api/bank-accounts/{id}
```

### Actualizar

```http
PUT /api/bank-accounts/{id}
Content-Type: application/json

{
  "name": "Bancolombia Ahorros Principal",
  "bankName": "Bancolombia",
  "accountNumber": "****1234",
  "accountType": "AHORROS",
  "notes": "Nota actualizada"
}
```

### Desactivar (soft delete)

```http
DELETE /api/bank-accounts/{id}
```

La cuenta no se borra físicamente. Se marca como `active: false`. Las conciliaciones históricas conservan la referencia.

---

## 3. Conciliación bancaria — flujo de uso

### Paso 0: Seleccionar cuenta y fecha

El frontend debe pedir al usuario:
1. **¿Qué cuenta bancaria?** → cargar desde `GET /api/bank-accounts` y que elija.
2. **¿Qué fecha?** → por defecto "hoy".

Toma nota del `bankAccountId` (el `id` de la cuenta elegida).

### Paso 1: Ver el resumen diario (readonly)

```http
GET /api/bank-reconciliation/daily-summary?date=2026-04-26&bankAccountId=664a1b2c3d4e5f6a7b8c9d0e
```

El sistema calcula y devuelve todas las transacciones no-efectivo del día para esa cuenta:

- **Ventas contado** con métodos ≠ EFECTIVO (TRANSFERENCIA, TARJETA_DÉBITO, TARJETA_CRÉDITO, CHEQUE, SALDO_FAVOR)
- **Abonos a crédito** (pagos de cuentas de clientes) en medios no-efectivo
- **Depósitos de anticipos** (saldo a favor) en medios no-efectivo
- **Devoluciones de anticipo** en medios no-efectivo (EGRESO)
- **Gastos** pagados en medios no-efectivo (EGRESO)
- **Pagos a proveedores** en medios no-efectivo (EGRESO)
- **Traslados de efectivo → banco** destinados específicamente a esta cuenta (INGRESO)

> ℹ️ Los traslados se filtran por `destinationBankAccountId`. Las demás transacciones (ventas, gastos, etc.) se muestran para **todos** los medios no-efectivo sin importar la cuenta destino, porque actualmente el sistema no registra a qué cuenta va cada pago de venta. Ver sección 6.

El campo clave es `expectedBankTotal`: **lo que el sistema dice que debería tener en el banco**.

### Paso 2: Ingresar saldo inicial

```http
GET /api/bank-reconciliation/suggested-opening?bankAccountId=664a1b2c3d4e5f6a7b8c9d0e
```

Devuelve el `totalBankCounted` del último cierre de esa cuenta. Úsalo como `openingBalance` sugerido.

```json
{
  "balance": 3800000.00,
  "lastCloseDate": "2026-04-25"
}
```

Si no hay cierres previos, devuelve `balance: 0`.

### Paso 3: Crear o actualizar la conciliación

```http
POST /api/bank-reconciliation
Content-Type: application/json

{
  "sessionDate": "2026-04-26",
  "bankAccountId": "664a1b2c3d4e5f6a7b8c9d0e",
  "openingBalance": 3800000.00,
  "totalBankCounted": 5200000.00,
  "notes": "Pendiente acreditación datáfono $450.000"
}
```

- `openingBalance`: saldo con el que empezó el día en esa cuenta.
- `totalBankCounted`: saldo que muestra la app/extracto bancario **al cierre del día**.

El sistema recalcula todos los totales y guarda con `status: EN_PROGRESO`.

Se puede llamar múltiples veces para actualizar (mientras no esté `CERRADO`).

**Respuesta:**
```json
{
  "id": "664b2c3d4e5f6a7b8c9d0e1f",
  "sessionDate": "2026-04-26",
  "bankAccountId": "664a1b2c3d4e5f6a7b8c9d0e",
  "bankAccountName": "Bancolombia Ahorros Principal",
  "openingBalance": 3800000.00,
  "totalBankCounted": 5200000.00,
  "expectedBankAmount": 1450000.00,
  "expectedBankTotal": 5250000.00,
  "difference": -50000.00,
  "totalIncome": 1900000.00,
  "totalExpense": 450000.00,
  "totalTransfers": 800000.00,
  "netBankFlow": 1450000.00,
  "status": "EN_PROGRESO",
  "notes": "Pendiente acreditación datáfono $450.000",
  "cancelReason": null,
  "auditTrail": [
    {
      "userId": "123456789",
      "userName": "Juan Pérez",
      "action": "APERTURA",
      "timestamp": "2026-04-26T18:00:00",
      "details": null
    }
  ]
}
```

El campo `difference: -50000` indica que el banco reporta $50.000 menos de lo esperado. En este caso, la nota explica el desfase por el datáfono pendiente.

### Paso 4: Cerrar la conciliación

```http
POST /api/bank-reconciliation/{id}/close
Content-Type: application/json

{
  "notes": "Aprobado por gerencia"
}
```

Una vez `CERRADO`:
- No se puede editar ni anular.
- El `totalBankCounted` de este cierre se convierte en el `openingBalance` sugerido del día siguiente.

### Paso 5 (opcional): Anular

```http
POST /api/bank-reconciliation/{id}/cancel
Content-Type: application/json

{
  "reason": "Error en la fecha, se creará una nueva"
}
```

Solo se puede anular si está `EN_PROGRESO`. No se puede anular una `CERRADO`.

---

## 4. Referencia de endpoints

### Cuentas bancarias

| Método   | Endpoint                       | Descripción                            | Body                        |
|----------|--------------------------------|----------------------------------------|-----------------------------|
| `GET`    | `/api/bank-accounts`           | Lista cuentas activas                  | —                           |
| `GET`    | `/api/bank-accounts/all`       | Lista todas (inc. inactivas)           | —                           |
| `GET`    | `/api/bank-accounts/{id}`      | Obtener por ID                         | —                           |
| `POST`   | `/api/bank-accounts`           | Crear cuenta                           | `CreateBankAccountRequest`  |
| `PUT`    | `/api/bank-accounts/{id}`      | Actualizar cuenta                      | `CreateBankAccountRequest`  |
| `DELETE` | `/api/bank-accounts/{id}`      | Desactivar cuenta (soft delete)        | —                           |

### Conciliación bancaria

| Método | Endpoint                                     | Params obligatorios          | Descripción                                         |
|--------|----------------------------------------------|------------------------------|-----------------------------------------------------|
| `GET`  | `/api/bank-reconciliation/daily-summary`     | `date`, `bankAccountId`      | Resumen diario calculado (sin guardar)              |
| `POST` | `/api/bank-reconciliation`                   | Body con `bankAccountId`     | Crear o actualizar una conciliación                 |
| `GET`  | `/api/bank-reconciliation/by-date`           | `date`, `bankAccountId`      | Obtener conciliación guardada por fecha y cuenta    |
| `GET`  | `/api/bank-reconciliation/{id}`              | —                            | Obtener por ID de sesión                            |
| `POST` | `/api/bank-reconciliation/{id}/close`        | —                            | Cerrar conciliación                                 |
| `POST` | `/api/bank-reconciliation/{id}/cancel`       | —                            | Anular conciliación                                 |
| `GET`  | `/api/bank-reconciliation`                   | Todos opcionales             | Listar conciliaciones con filtros                   |
| `GET`  | `/api/bank-reconciliation/suggested-opening` | `bankAccountId`              | Saldo apertura sugerido del último cierre           |

#### Filtros del listado

```
GET /api/bank-reconciliation?fromDate=2026-04-01&toDate=2026-04-30&status=CERRADO&bankAccountId=664a...
```

Todos los parámetros son opcionales. Si se omite `bankAccountId`, devuelve conciliaciones de todas las cuentas.

---

## 5. Lógica de cálculo

```
expectedBankAmount = totalIncome(no-efectivo) - totalExpense(no-efectivo)
                   ↑ incluye traslados a ESTA cuenta como INGRESO

expectedBankTotal  = openingBalance + expectedBankAmount

difference         = totalBankCounted - expectedBankTotal
                     > 0 → sobrante en banco
                     < 0 → faltante en banco
                     = 0 → cuadrado
```

### Fuentes de INGRESOS

| Fuente                   | Filtro aplicado                                      |
|--------------------------|------------------------------------------------------|
| Ventas contado           | `paymentMethod != EFECTIVO`                         |
| Abonos a crédito         | `paymentMethod != EFECTIVO`                         |
| Depósitos de anticipos   | `paymentMethod != EFECTIVO`                         |
| Traslados caja → banco   | `destinationBankAccountId == bankAccountId` del request |

### Fuentes de EGRESOS

| Fuente                  | Filtro aplicado                 |
|-------------------------|---------------------------------|
| Gastos                  | `paymentMethod != EFECTIVO`     |
| Pagos a proveedores     | `paymentMethod != EFECTIVO`     |
| Devoluciones de anticipo| `paymentMethod != EFECTIVO`     |

---

## 6. Limitaciones actuales y hoja de ruta

### Limitación principal

Las entidades `Billing` (ventas), `Expense`, `SupplierPayment`, `AccountPayment` y `CreditTransaction` **no tienen campo `bankAccountId`**. Por lo tanto:

- Las ventas con TRANSFERENCIA, los gastos con datáfono, etc., **no se pueden atribuir a una cuenta específica**.
- Aparecen en el resumen de **todas** las cuentas por igual.
- Solo los `InternalTransfer` (traslados físicos de caja → banco) se filtran estrictamente por cuenta destino.

### Consecuencia práctica

Si el negocio tiene 2 cuentas (A y B) y recibe una transferencia de $500.000 de un cliente, esa venta aparece en el resumen tanto de A como de B. El usuario debe ingresar el `totalBankCounted` real de cada cuenta para que la `difference` tenga sentido por separado.

### Hoja de ruta (Phase 2)

Para lograr atribución precisa por cuenta, el frontend deberá:

1. Cuando se registre un **pago** en ventas, abonos, gastos o pagos a proveedores con método ≠ EFECTIVO, solicitar al usuario que seleccione la **cuenta bancaria destino**.
2. El backend almacenará ese `bankAccountId` en:
   - `PaymentEntry.bankAccountId` (en `Billing`)
   - `AccountPayment.bankAccountId` (en `ClientAccount`)
   - `CreditTransaction.bankAccountId` (en `ClientCredit`)
   - `Expense.bankAccountId`
   - `SupplierPayment.bankAccountId`
3. Los métodos `getSalesTransactions`, `getCreditPaymentTransactions`, etc. en `BankReconciliationServiceImpl` se actualizarán para filtrar por `bankAccountId`.

---

## Notas para el frontend

- La autenticación es la misma de todo el sistema (JWT). No se requieren headers adicionales.
- Fechas: formato ISO `YYYY-MM-DD`.
- Montos: `number` con 2 decimales (BigDecimal en backend).
- `totalBankCounted` es lo que el usuario lee en su app bancaria/extracto — **no** es calculado por el sistema.
- Una diferencia ≠ 0 es normal (datáfonos acreditan con 1-3 días de desfase). Permitir guardar con nota.
- El `openingBalance` del día = `totalBankCounted` del último cierre de esa misma cuenta.
