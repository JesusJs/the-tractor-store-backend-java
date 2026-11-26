# Contrato: Inventory y Cart API (004, P1)

Fuente: `docs/api-contract.md` (secciones Inventory y Cart) y la ADR 0011. Aquí no se añade ningún
campo ni código. Todo error usa el formato único `{ code, message, details?, traceId }` + cabecera
`X-Trace-Id` (contrato de 002, `specs/002-catalogo/contracts/errors.md`).

## Cookie `ts_session`

| Atributo | Valor |
|---|---|
| Nombre | `ts_session` |
| Valor | UUID v4 aleatorio generado por el backend, canónico en minúsculas |
| `Path` | `/` |
| `HttpOnly` | sí |
| `SameSite` | `Lax` |
| `Secure` | solo con `tractorstore.session.secure-cookie=true` (perfil `prod`) |
| `Max-Age` / `Expires` | no se envían (cookie de sesión del navegador) |

Ejemplo de cabecera: `Set-Cookie: ts_session=3f0c…e9a1; Path=/; HttpOnly; SameSite=Lax`.

| Petición trae… | `GET /cart` y `POST /orders` | `GET /orders/{id}` |
|---|---|---|
| nada | crea cookie nueva | no crea; responde 404 |
| UUID canónico | lo usa; **sin** `Set-Cookie` | lo usa |
| valor no UUID | lo ignora; crea cookie nueva | lo trata como sin cookie → 404 |

---

## GET /api/v1/inventory/{sku}

Sin sesión: no lee ni crea `ts_session`.

| Caso | Status | Cuerpo |
|---|---|---|
| SKU con fila de stock | 200 | `{ "sku": "CL-01-GR", "stock": 10 }` |
| SKU con stock 0 | 200 | `{ "sku": "CL-01-GY", "stock": 0 }` |
| SKU sin fila (desconocido) | 404 | `{ "code": "VARIANT_NOT_FOUND", "message": "Variant XX-99-ZZ not found", "traceId": "…" }` |
| `sku` > 20 caracteres o con caracteres fuera de `[A-Za-z0-9-]` (p. ej. `%3Cb%3Ex`; review del PR #5, A06) | 400 | `{ "code": "VALIDATION_ERROR", "message": "Request validation failed", "details": { "sku": "…" }, "traceId": "…" }` |

Tipos: `sku` string, `stock` entero ≥ 0.

---

## GET /api/v1/cart

| Caso | Status | Cuerpo | Cookie |
|---|---|---|---|
| Cualquiera | 200 | `{ "items": [], "totalItems": 0, "subTotal": 0, "tax": 0, "total": 0 }` | según la tabla de la cookie |

- El front lo llama con `withCredentials: true` y `responseType: 'text'`, e ignora cuerpo y
  errores. Solo le importa la cookie.
- `subTotal`, `tax` y `total` son números. El backend puede serializarlos como `0` o `0.00`; el
  front no los lee.
- OpenAPI documenta `items` como `CartItem[]`
  (`{ productId, variantId, productName, variantName, price, quantity, image }`), aunque siempre
  llega vacío.

## OpenAPI (FR-031)

Tags `Inventory` y `Cart`. Cada operación lleva `@Operation` (resumen y descripción, incluida la
cookie) y `@ApiResponse` por status: 200/400/404/500 en inventario y 200/500 en carrito. Los errores
usan el esquema `ApiError`.
