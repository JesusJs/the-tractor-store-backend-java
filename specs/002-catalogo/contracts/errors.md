# Contrato — errores, traza y CORS (002)

Transversal: lo implementa el módulo `shared` y lo reutilizarán inventario, carrito y pedidos.

## Forma única (FR-014)

```json
{ "code": "VALIDATION_ERROR", "message": "Request validation failed",
  "details": { "id": "size must be between 0 and 20" }, "traceId": "4bf92f3577b34da6a3ce929d0e0e4736" }
```

- `details` solo aparece si aporta información (validación); si no, la clave **no** se envía.
- `Content-Type: application/json`.
- La cabecera `X-Trace-Id` lleva el mismo valor que `traceId`.

## Tabla de códigos

| HTTP | `code` | Cuándo | `details` |
|---|---|---|---|
| 400 | `VALIDATION_ERROR` | Bean Validation en cuerpo (`@Valid`) o en path/query con constraints (p. ej. `id`/`filter` > 20 caracteres, `skus` > 50 elementos o con un elemento > 20) | campo/parámetro → mensaje |
| 400 | `MALFORMED_REQUEST` | cuerpo ilegible, tipo de parámetro incorrecto, parámetro obligatorio ausente | — |
| 404 | `PRODUCT_NOT_FOUND` | `GET /products/{id}` con id inexistente | — |
| 404 | `CATEGORY_NOT_FOUND` | `GET /categories/{filter}` con filtro no válido | — |
| 404 | `VARIANT_NOT_FOUND` | reservado para carrito (ningún endpoint de catálogo lo devuelve) | — |
| 404 | `NOT_FOUND` | **nuevo** — ruta sin handler (p. ej. `/api/v1/catalog/products`) | — |
| 405 | `METHOD_NOT_ALLOWED` | **nuevo** — método no soportado (p. ej. `POST /api/v1/catalog/home`) | — |
| 500 | `INTERNAL_ERROR` | cualquier otro fallo (incl. BD caída); mensaje genérico `Unexpected error`, sin stacktrace, clases ni SQL | — |

Los códigos `NOT_FOUND` y `METHOD_NOT_ALLOWED` se añaden a `docs/api-contract.md` en la ADR 0008.

## Traza (FR-015)

| Petición | Respuesta |
|---|---|
| sin `X-Trace-Id` | se genera uno (32 hex) y va en la cabecera de la respuesta (éxito o error) y en `traceId` |
| con `X-Trace-Id` válido (`^[A-Za-z0-9._-]{1,64}$`) | se reutiliza tal cual |
| con `X-Trace-Id` inválido (vacío, > 64, caracteres raros) | se ignora y se genera uno nuevo |

Cada línea de log emitida durante la petición incluye `[<traceId>]` (MDC `traceId`).

## CORS (FR-027)

Aplica a `/api/**`. Orígenes de `CORS_ALLOWED_ORIGINS` (coma); en el perfil `dev`, por defecto
`http://localhost:4200,http://localhost:4201,http://localhost:4202,http://localhost:4203`.

| Petición | Respuesta |
|---|---|
| `Origin` permitido (simple o preflight `OPTIONS`) | `Access-Control-Allow-Origin: <origin>`, `Access-Control-Allow-Credentials: true`; `Access-Control-Expose-Headers` incluye `X-Trace-Id` |
| `Origin` no permitido | **403** `Invalid CORS request` (respuesta estándar de Spring, fuera del formato único; ver research R10), sin `Access-Control-Allow-Origin` |
| sin `Origin` (curl, tests) | sin cambios, sin cabeceras CORS |
| `CORS_ALLOWED_ORIGINS` contiene `*` | la aplicación no arranca |

Métodos permitidos: `GET, POST, DELETE, OPTIONS`. Cabeceras permitidas: `Content-Type, X-Trace-Id`.
`max-age` 3600.
