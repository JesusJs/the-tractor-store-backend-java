# 0008 — El contrato de catálogo lo manda el front

- Estado: aceptada (aprobada 2026-11-19)
- Fecha: 2026-11-19

## Contexto
La spec 002 revisó el front Angular (`packages/shared-catalog`: `catalog.models.ts`, `catalog.service.ts`, `product.service.ts`). La forma que consume es la que devolvía el backend .NET anterior y **difiere en casi todo** de la sección Catalog de `docs/api-contract.md` (tabla "Contexto" de `specs/002-catalogo/spec.md`):

| Endpoint | `docs/api-contract.md` decía | El front consume |
|---|---|---|
| `GET /home` | objeto `{ teasers, strategy }` | array `[{ id, title, image, filter }]` |
| `GET /categories/{filter}` | `{ title, filters, products: [ProductSummary] }` | `{ category, products: [ProductItem], availableFilters }` |
| Ítem de listado | `{ id, name, image, url, startPrice }` | `{ id, name, brand, price, image, variants: string[], description }` |
| `GET /products/{id}` | variantes ricas (sku, name, image, color, price) | `{ id, name, brand, price, image, description, variants: string[], highlights }` |
| `GET /recommendations` | `[{ sku, name, image, url }]` | array de ítems de listado; `variants[0]` = SKU recomendado |
| `GET /stores` | `{ ..., street, ... }` | `{ ..., address, ... }` |

El front no cambia (CLAUDE.md), así que el contrato debe ajustarse a él. El contrato exige ADR para cualquier cambio.

## Decisión
La sección Catalog de `docs/api-contract.md` se reescribe con la forma exacta del front, detallada en `specs/002-catalogo/contracts/catalog-api.md`. Además:
- Se añaden dos códigos de error genéricos: **`NOT_FOUND`** (404, ruta sin handler) y **`METHOD_NOT_ALLOWED`** (405, método no soportado).
- `GET /recommendations` valida `skus` con `@Size(max = 50)` en la lista y `@Size(max = 20)` por elemento: solo una entrada abusiva recibe 400 `VALIDATION_ERROR`; nunca hay errores de negocio.
- `brand` = etiqueta de la categoría; `description` = highlights sin `.` final unidos con `". "`; `price` = precio "desde" (mínimo de las variantes).

## Alternativas consideradas
- **Mantener el contrato documentado y adaptar el front**: prohibido, el front no cambia.
- **Servir ambas formas** (versionado o negociación): duplica código sin consumidor para la forma antigua.

## Consecuencias
- Positivas:
  - El front funciona sin cambios contra el backend Java.
  - El contrato vuelve a ser fuente de verdad real (se retiran las marcas ⚠️ de catálogo).
- Negativas / deuda aceptada (limitaciones conocidas):
  - **Precio "desde"** (research R2): el `price` de listado/detalle es el mínimo de las variantes; p. ej. `AU-07-YE` cuesta 5000 y se muestra 4000. El carrito (spec 007) usará el precio real de la variante.
  - **Rechazo CORS fuera del formato único** (research R10): un `Origin` no permitido recibe el 403 estándar de Spring con cuerpo `Invalid CORS request`. El navegador nunca entrega esa respuesta al front.
  - **Dependencia del CDN externo** (research R3): las imágenes son URLs absolutas de `https://blueprint.the-tractor.store/cdn/img/...`. Si el CDN cae, el catálogo se ve sin imágenes; cambiar de origen es una migración `UPDATE` nueva.
- Cómo se verifica: `CatalogControllerTest` (forma JSON por endpoint), `CatalogApiIT` (E2E con dataset real), `GlobalExceptionHandlerTest` (códigos genéricos) y validación con el front real (quickstart §6).
