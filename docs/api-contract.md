# Contrato de API — Tractor Store backend

Fuente de verdad del contrato con el front Angular. **Cualquier cambio aquí necesita ADR.**
Los campos marcados con ⚠️ hay que confirmarlos contra los modelos TypeScript del front (`shared-catalog`) antes de implementar la spec correspondiente.

## Convenciones
- Prefijo: **`/api/v1`**. JSON en UTF-8. Dinero como número decimal (en BD `NUMERIC(10,2)`), sin IVA (P7).
- Estado del carrito y de los pedidos ligado a la cookie de sesión **`ts_session`** (HttpOnly, `SameSite=Lax`, `Secure` en prod). Si no existe, el backend la crea en la primera petición a `/cart` u `/orders`. Sin login, JWT ni Keycloak (P5).
- Un pedido de otra sesión → **404** (no 403: no revelamos que existe).
- El precio **siempre** sale del catálogo; el cliente nunca envía precios.

## Error único
```json
{ "code": "PRODUCT_NOT_FOUND", "message": "Product CL-99 not found", "details": { "field": "reason" }, "traceId": "4bf92f35..." }
```
| HTTP | code (ejemplos) |
|---|---|
| 400 | `VALIDATION_ERROR` (details = errores por campo/parámetro), `MALFORMED_REQUEST` (cuerpo ilegible, tipo incorrecto, parámetro obligatorio ausente) |
| 404 | `PRODUCT_NOT_FOUND`, `VARIANT_NOT_FOUND`, `STORE_NOT_FOUND`, `ORDER_NOT_FOUND`, `CATEGORY_NOT_FOUND`, `NOT_FOUND` (ruta sin handler) |
| 405 | `METHOD_NOT_ALLOWED` (método no soportado) |
| 409 | `OUT_OF_STOCK`, `CART_EMPTY` |
| 500 | `INTERNAL_ERROR` (mensaje `Unexpected error`, sin stacktrace, clases ni SQL) |

- `details` solo aparece cuando aporta información (validación); si no, la clave no se envía.
- `NOT_FOUND` y `METHOD_NOT_ALLOWED` se añadieron en la [ADR 0008](adr/0008-contrato-catalogo-segun-front.md).
- `traceId` también va en la cabecera `X-Trace-Id` de **toda** respuesta (éxito o error). Si el front envía un `X-Trace-Id` válido (`^[A-Za-z0-9._-]{1,64}$`) se respeta; si no, se genera uno (32 hex).
- Excepción conocida: un `Origin` no permitido por CORS recibe el 403 estándar de Spring (`Invalid CORS request`), fuera de este formato (ADR 0008).

## Endpoints (12)

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
| GET | `/api/v1/inventory/{sku}` | `{ sku, stock, inStock }` — SKU desconocido → 404 |
|---|---|---|

### Cart (cookie `ts_session`)
| Método | Ruta | Cuerpo | Respuesta |
|---|---|---|---|
| GET | `/api/v1/cart` | — | `Cart` (vacío si no hay) |
| GET | `/api/v1/cart/mini` | — | `{ quantity }` |
| POST | `/api/v1/cart/items` | `{ sku }` | `Cart` — si ya existe, suma 1; sin stock → 409 |
| DELETE | `/api/v1/cart/items/{sku}` | — | `Cart` — **elimina solo ese SKU** |

### Order (cookie `ts_session`)
| Método | Ruta | Cuerpo | Respuesta |
|---|---|---|---|
| POST | `/api/v1/orders` | `{ firstName, lastName, storeId, email? }` | **201** + `Location: /api/v1/orders/{id}` + `Order`; carrito vacío → 409 |
| GET | `/api/v1/orders/{id}` | — | `Order`; otra sesión o inexistente → 404 |

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
- `price` = mínimo de los precios de las variantes. Limitación conocida: `AU-07-YE` cuesta 5000 y se muestra 4000; el carrito usará el precio real de la variante.

Carrito y pedidos (⚠️ confirmar nombres de campos con el front en sus specs):
```ts
LineItem       { sku: string; name: string; image: string; quantity: number; unitPrice: number; total: number }
Cart           { lineItems: LineItem[]; total: number; skus: string[] }
Order          { id: string; status: 'PLACED'; firstName: string; lastName: string; storeId: string;
                 lineItems: LineItem[] /* con SKU, no solo nombre */; total: number; createdAt: string }
```

## Recomendaciones por color
Se normalizan los SKUs recibidos (`trim`, sin vacíos, sin duplicados, sin desconocidos). Si no queda ninguno → lista por defecto (primera variante de los 4 primeros productos: `CL-01-GR`, `CL-02-BL`, `CL-03-GR`, `CL-04-BL`). Si no: color promedio de los SKUs conocidos; las demás variantes se ordenan por **distancia euclidiana RGB** a ese color (empate por SKU), se queda la más cercana de cada producto y se devuelven **4**. La categoría no interviene. Es lógica de dominio pura → test unitario parametrizado.
