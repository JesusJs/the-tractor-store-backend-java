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
| 400 | `VALIDATION_ERROR` (details = errores por campo), `MALFORMED_REQUEST` |
| 404 | `PRODUCT_NOT_FOUND`, `VARIANT_NOT_FOUND`, `STORE_NOT_FOUND`, `ORDER_NOT_FOUND`, `CATEGORY_NOT_FOUND` |
| 409 | `OUT_OF_STOCK`, `CART_EMPTY` |
| 500 | `INTERNAL_ERROR` (sin stacktrace) |

`traceId` también va en la cabecera `X-Trace-Id` de la respuesta; si el front envía `X-Trace-Id`, se respeta.

## Endpoints (12)

### Catalog
| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/api/v1/catalog/home` | `{ teasers: [{ title, image, url }], strategy: [...] }` ⚠️ |
| GET | `/api/v1/catalog/categories/{filter}` | `{ title, filters: [{ url, name, active }], products: [ProductSummary] }` — `filter` ∈ `all \| classic \| autonomous` ⚠️ |
| GET | `/api/v1/catalog/products/{id}` | `Product` |
| GET | `/api/v1/catalog/recommendations?skus=AU-01-SI,CL-02-GR` | `[Recommendation]` — sin `skus` o vacío → lista por defecto, **nunca error** |
| GET | `/api/v1/catalog/stores` | `[Store]` |

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

## Modelos (⚠️ confirmar nombres de campos con el front)
```ts
ProductSummary { id: string; name: string; image: string; url: string; startPrice: number }
Product        { id: string; name: string; category: 'classic'|'autonomous'; highlights: string[]; variants: Variant[] }
Variant        { sku: string; name: string; image: string; color: string /* hex */; price: number }
Recommendation { sku: string; name: string; image: string; url: string }
Store          { id: string; name: string; street: string; city: string; image: string }
LineItem       { sku: string; name: string; image: string; quantity: number; unitPrice: number; total: number }
Cart           { lineItems: LineItem[]; total: number; skus: string[] }
Order          { id: string; status: 'PLACED'; firstName: string; lastName: string; storeId: string;
                 lineItems: LineItem[] /* con SKU, no solo nombre */; total: number; createdAt: string }
```

## Recomendaciones por color
Para cada SKU recibido se toma el color de su variante; se ordenan las demás variantes por **distancia euclidiana RGB** al color promedio de los SKUs recibidos; se excluyen los SKUs recibidos y se devuelven las N más cercanas (N = 4 ⚠️). Es lógica de dominio pura → test unitario parametrizado.
