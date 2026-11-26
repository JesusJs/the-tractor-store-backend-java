# Contrato: Order API (004, P2)

Fuente: `docs/api-contract.md` (sección Order y Modelos) y la ADR 0011 (enmendada: `storeId` se
valida). Cookie `ts_session`: ver [inventory-cart-api.md](inventory-cart-api.md#cookie-ts_session).

## POST /api/v1/orders

### Petición: cuerpo exacto que envía `cart.service.ts`
```json
{
  "firstName": "Ada",
  "lastName": "Lovelace",
  "storeId": "store-a",
  "extraPickups": "Big Micro Machines, Central Mall",
  "items": [
    {
      "productId": "AU-07",
      "variantId": "AU-07-YE",
      "productName": "Verde Voyager",
      "variantName": "AU-07-YE",
      "price": 4000,
      "quantity": 1,
      "image": "https://blueprint.the-tractor.store/cdn/img/product/[size]/AU-07-YE.webp"
    }
  ]
}
```
- Solo se usan `firstName`, `lastName`, `storeId`, `extraPickups`, `items[].variantId` e
  `items[].quantity`. El resto de campos de línea (`productId`, `productName`, `variantName`,
  `price`, `image`) y cualquier campo desconocido **se ignoran** (ni se validan ni se enlazan).
- `extraPickups`: string opcional. Si falta, es `""` o solo espacios, se trata como ausente.

Límites (Bean Validation, 400 `VALIDATION_ERROR`, `details` = ruta del campo → mensaje):

| Campo | Regla |
|---|---|
| `firstName`, `lastName` | obligatorio, no en blanco, ≤ 100, sin caracteres de control (review #4) |
| `storeId` | obligatorio, no en blanco, ≤ 20, solo letras, dígitos y `-` (review #4) |
| `extraPickups` | ≤ 500, sin caracteres de control (review #4) |
| `items` | ≤ 50 elementos; ningún elemento `null` |
| `items[i].variantId` | obligatorio, no en blanco, ≤ 20, solo letras, dígitos y `-` (review #4) |
| `items[i].quantity` | obligatorio, entero 1..99 |

### Respuesta 201
Cabeceras: `Location: /api/v1/orders/{id}`, `X-Trace-Id`, y `Set-Cookie: ts_session=…` si la
petición no traía una cookie válida.

```json
{
  "id": "0b8e6a8e-5d0f-4a51-9d3c-2f7f3f1a7c11",
  "firstName": "Ada",
  "lastName": "Lovelace",
  "storeId": "store-a",
  "extraPickups": ["Big Micro Machines", "Central Mall"],
  "items": [
    {
      "productId": "AU-07",
      "variantId": "AU-07-YE",
      "productName": "Verde Voyager",
      "variantName": "AU-07-YE",
      "price": 5000.00,
      "quantity": 1,
      "image": "<imagen de la variante en el catálogo>"
    }
  ],
  "subTotal": 5000.00,
  "tax": 1050.00,
  "total": 6050.00,
  "placedAt": "2026-11-24T21:15:03.123Z",
  "status": "PLACED"
}
```
- `items[].price` es el precio **real** de la variante. `productId`, `productName` e `image` salen
  del catálogo, y `variantName` = `variantId` = SKU.
- `subTotal` = Σ `price × quantity`; `tax` = `subTotal × 0.21`, 2 decimales, `HALF_UP`;
  `total` = `subTotal + tax`.
- `extraPickups`: lista partida por `", "`, sin fragmentos vacíos; `[]` si no llegó. Nunca `null`.
- `placedAt`: ISO-8601 UTC con milisegundos.

### Errores y precedencia (se devuelve el primero que aplica)

| # | Condición | Status | `code` | Efecto |
|---|---|---|---|---|
| 1 | Cuerpo ilegible, JSON inválido, tipo incorrecto (p. ej. `extraPickups` array, `quantity` texto) | 400 | `MALFORMED_REQUEST` | nada guardado |
| 2 | Falla un límite de la tabla anterior | 400 | `VALIDATION_ERROR` (+ `details`) | nada guardado |
| 3 | `items` ausente o `[]` | 409 | `CART_EMPTY` | nada guardado |
| 4 | `storeId` no es el `id` de una tienda del catálogo (exacto) | 404 | `STORE_NOT_FOUND` | ni pedido ni descuentos |
| 5 | Algún `variantId` no existe en el catálogo (se informa el primero en orden de líneas) | 404 | `VARIANT_NOT_FOUND` | ni pedido ni descuentos |
| 6 | Alguna línea sin stock suficiente (o SKU sin fila de stock) | 409 | `OUT_OF_STOCK` | **rollback total**: ni pedido ni descuentos |
| — | Fallo inesperado (BD caída…) | 500 | `INTERNAL_ERROR` | rollback total |

Las líneas del mismo SKU se procesan una a una, y cada una descuenta su cantidad.

## GET /api/v1/orders/{id}

| Caso | Status | Cuerpo |
|---|---|---|
| Pedido de la sesión de la cookie | 200 | `OrderReceipt` **idéntico** al devuelto por el `POST` |
| Otra sesión, sin cookie, cookie no UUID, id inexistente o id que no es UUID canónico | 404 | `{ "code": "ORDER_NOT_FOUND", "message": "Order not found", "traceId": "…" }` |

- Nunca responde 403 ni 400. No crea cookie.
- El `message` es siempre el mismo y no repite el id recibido: así no refleja entradas arbitrarias
  y no distingue un caso de otro.

## OpenAPI (FR-031)

Tag `Orders`. `POST`: 201 (con las cabeceras `Location` y `Set-Cookie` descritas), 400, 404, 409 y
500. `GET`: 200, 404 y 500. El esquema de petición documenta los campos ignorados como "ignored;
taken from the catalog".
