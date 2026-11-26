# Contrato de API — Tractor Store backend

Fuente de verdad del contrato con el front Angular. **Cualquier cambio aquí necesita ADR.**
Todas las secciones están confirmadas contra el front (`shared-catalog`, `mfe-checkout`): catálogo en la ADR 0008; inventario, carrito y pedidos en la [ADR 0011](adr/0011-contrato-carrito-y-pedidos-segun-front.md).

## Convenciones
- Prefijo: **`/api/v1`**. JSON en UTF-8. Dinero como número decimal (en BD `NUMERIC(10,2)`, `BigDecimal` en Java). Los pedidos incluyen IVA del 21 % calculado en el backend (ADR 0011; deja sin efecto el "sin IVA" de P7).
- Los pedidos se ligan a la cookie de sesión **`ts_session`** (HttpOnly, `SameSite=Lax`, `Secure` en prod). Si no existe, el backend la crea en `GET /cart` y en `POST /orders`. El carrito vive en `localStorage` del front; no hay carrito en servidor (ADR 0011). Sin login, JWT ni Keycloak (P5).
- Un pedido de otra sesión → **404** (no 403: no revelamos que existe).
- El precio **siempre** sale del catálogo: el front envía `price` en las líneas de pedido y el backend lo **ignora**.

## Error único
```json
{ "code": "PRODUCT_NOT_FOUND", "message": "Product CL-99 not found", "details": { "field": "reason" }, "traceId": "4bf92f35..." }
```
| HTTP | code (ejemplos) |
|---|---|
| 400 | `VALIDATION_ERROR` (details = errores por campo/parámetro), `MALFORMED_REQUEST` (cuerpo ilegible, tipo incorrecto, parámetro obligatorio ausente) |
| 404 | `PRODUCT_NOT_FOUND`, `VARIANT_NOT_FOUND`, `STORE_NOT_FOUND`, `ORDER_NOT_FOUND`, `CATEGORY_NOT_FOUND`, `NOT_FOUND` (ruta sin handler) |
| 405 | `METHOD_NOT_ALLOWED` (método no soportado) |
| 406, 415… | `MALFORMED_REQUEST` (otros errores de cliente de Spring MVC, con su status; review 002 #1) |
| 409 | `OUT_OF_STOCK`, `CART_EMPTY` |
| 500 | `INTERNAL_ERROR` (mensaje `Unexpected error`, sin stacktrace, clases ni SQL) |

- `details` solo aparece cuando aporta información (validación); si no, la clave no se envía.
- `NOT_FOUND` y `METHOD_NOT_ALLOWED` se añadieron en la [ADR 0008](adr/0008-contrato-catalogo-segun-front.md).
- `traceId` también va en la cabecera `X-Trace-Id` de **toda** respuesta (éxito o error). Si el front envía un `X-Trace-Id` válido (`^[A-Za-z0-9._-]{1,64}$`) se respeta; si no, se genera uno (32 hex).
- Excepción conocida: un `Origin` no permitido por CORS recibe el 403 estándar de Spring (`Invalid CORS request`), fuera de este formato (ADR 0008).

## Endpoints (9)

### Catalog
Forma exacta del front ([ADR 0008](adr/0008-contrato-catalogo-segun-front.md); detalle y ejemplos en `specs/002-catalogo/contracts/catalog-api.md`). Solo `GET`, sin estado (no usa `ts_session`). Ninguna respuesta incluye `stock`, `enginePower` ni colores. `image` es siempre una URL absoluta del CDN `https://blueprint.the-tractor.store/cdn/img/...`.

| Método | Ruta | Respuesta | Errores |
|---|---|---|---|
| GET | `/api/v1/catalog/home` | `[Teaser]` (2: `classic`, `autonomous`) | — |
| GET | `/api/v1/catalog/categories/{filter}` | `CategoryListing` — `filter` ∈ `all \| classic \| autonomous` (exacto, sensible a mayúsculas) | 404 `CATEGORY_NOT_FOUND`; 400 `VALIDATION_ERROR` si `filter` > 20 caracteres |
| GET | `/api/v1/catalog/products/{id}` | `ProductDetail` | 404 `PRODUCT_NOT_FOUND`; 400 `VALIDATION_ERROR` si `id` > 20 caracteres |
| GET | `/api/v1/catalog/recommendations?skus=AU-01-SI,CL-02-GR` | `[ProductItem]` (hasta 4; `variants[0]` = SKU recomendado) — sin `skus`, vacío o solo desconocidos → lista por defecto, **nunca error de negocio** | 400 `VALIDATION_ERROR` (`details.skus`) si > 50 SKUs o alguno > 20 caracteres |
| GET | `/api/v1/catalog/stores` | `[Store]` (4) | — |

### Inventory
| Método | Ruta | Respuesta | Errores |
|---|---|---|---|
| GET | `/api/v1/inventory/{sku}` | `{ sku, stock }` | 404 `VARIANT_NOT_FOUND` (SKU desconocido); 400 `VALIDATION_ERROR` (SKU > 20 caracteres o con caracteres que no son letras, dígitos ni guiones; el SKU no se refleja en la respuesta) |

Sin sesión. El front muestra "Out of stock" si `stock === 0`.

### Cart (cookie `ts_session`)
| Método | Ruta | Cuerpo | Respuesta |
|---|---|---|---|
| GET | `/api/v1/cart` | — | 200 `Cart` **siempre vacío**; crea `ts_session` si falta o no es un UUID válido |

El front solo lo llama para que el backend fije la cookie (`responseType: 'text'`, ignora cuerpo y errores). `GET /cart/mini`, `POST /cart/items` y `DELETE /cart/items/{sku}` se retiraron del contrato de forma temporal (ADR 0011): vuelven con la spec 005\* `carrito-servidor` (2026-12-01).

### Order (cookie `ts_session`)
| Método | Ruta | Cuerpo | Respuesta |
|---|---|---|---|
| POST | `/api/v1/orders` | `OrderPayload` | **201** + `Location: /api/v1/orders/{id}` + `OrderReceipt`; crea `ts_session` si falta |
| GET | `/api/v1/orders/{id}` | — | `OrderReceipt`; otra sesión o inexistente → 404 `ORDER_NOT_FOUND` |

Reglas de `POST /orders` (ADR 0011):
- De cada línea solo se usan `variantId` (= SKU) y `quantity`. `price`, `productId`, `productName`, `variantName` e `image` se ignoran y salen de `catalog.api` (precio real de la variante, no el "desde").
- `firstName` y `lastName` se guardan tal cual. `storeId` se valida contra las tiendas del catálogo (`catalog.api`) y se guarda tal cual. `extraPickups` es texto libre (el front lo envía como `"a, b"` o lo omite): se guarda tal cual y el recibo lo devuelve partido por `", "`, `[]` si no llegó, nunca `null`.
- `subTotal` = Σ precio × cantidad; `tax` = `subTotal × 0.21` redondeado a 2 decimales `HALF_UP`; `total` = `subTotal + tax`.
- Stock: se valida y descuenta en la misma transacción con un `UPDATE` condicional (`stock >= qty`); si una línea falla, se revierte todo.
- Errores: `items` vacío → 409 `CART_EMPTY`; `variantId` vacío → 400 `VALIDATION_ERROR`; `storeId` inexistente → 404 `STORE_NOT_FOUND`; SKU inexistente → 404 `VARIANT_NOT_FOUND`; `quantity < 1` → 400 `VALIDATION_ERROR`; stock insuficiente → 409 `OUT_OF_STOCK`.

## Modelos
Catálogo (confirmado contra `catalog.models.ts` del front, ADR 0008):
```ts
Teaser         { id: string; title: string; image: string; filter: 'classic'|'autonomous' }
ProductItem    { id: string; name: string; brand: 'Classic'|'Autonomous'; price: number /* "desde" */;
                 image: string; variants: string[] /* SKUs */; description: string }
CategoryListing{ category: 'all'|'classic'|'autonomous'; products: ProductItem[];
                 availableFilters: ['all','classic','autonomous'] }
ProductDetail  { id: string; name: string; brand: 'Classic'|'Autonomous'; price: number; image: string;
                 description: string; variants: string[] /* SKUs */; highlights: string[] }
Store          { id: string; name: string; address: string; city: string; image: string }
```
- `brand` = etiqueta de la categoría; `description` = highlights sin `.` final unidos con `". "`.
- `price` = mínimo de los precios de las variantes. Limitación conocida: `AU-07-YE` cuesta 5000 y se muestra 4000; el carrito del front usa este precio "desde" y el recibo el real de la variante hasta la spec 005\* `carrito-servidor` (ADR 0011, desviación temporal).

Inventario, carrito y pedidos (confirmado contra `catalog.models.ts` y `cart.service.ts` del front, ADR 0011):
```ts
Stock            { sku: string; stock: number }
CartItem         { productId: string; variantId: string /* SKU */; productName: string; variantName: string;
                   price: number; quantity: number; image: string }
Cart             { items: CartItem[]; totalItems: number; subTotal: number; tax: number; total: number }
                 // GET /cart → { items: [], totalItems: 0, subTotal: 0, tax: 0, total: 0 }
OrderPayloadItem { productId: string; variantId: string /* SKU */; productName: string; variantName: string;
                   price: number /* ignorado */; quantity: number; image: string }
OrderPayload     { firstName: string; lastName: string; storeId: string;
                   extraPickups?: string /* "a, b"; el tipo TS dice string[] pero envía string */;
                   items: OrderPayloadItem[] }
OrderReceiptItem { productId: string; variantId: string; productName: string; variantName: string /* = SKU */;
                   price: number /* precio real de la variante */; quantity: number; image: string }
OrderReceipt     { id: string; firstName: string; lastName: string; storeId: string; extraPickups: string[];
                   items: OrderReceiptItem[]; subTotal: number; tax: number; total: number;
                   placedAt: string /* ISO-8601 */; status: 'PLACED' }
```
- Limitación conocida: el carrito del front calcula con el precio "desde" y el recibo con el real (`AU-07-YE`: 4000 en el carrito, 5000 en el recibo). Deuda aceptada en la ADR 0011.

## Recomendaciones por color
Se normalizan los SKUs recibidos (`trim`, sin vacíos, sin duplicados, sin desconocidos). Si no queda ninguno → lista por defecto (primera variante de los 4 primeros productos: `CL-01-GR`, `CL-02-BL`, `CL-03-GR`, `CL-04-BL`). Si no: color promedio de los SKUs conocidos; las demás variantes se ordenan por **distancia euclidiana RGB** a ese color (empate por SKU), se queda la más cercana de cada producto y se devuelven **4**. La categoría no interviene. Es lógica de dominio pura → test unitario parametrizado.
