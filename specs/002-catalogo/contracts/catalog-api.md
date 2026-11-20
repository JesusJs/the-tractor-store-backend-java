# Contrato — Catalog API (002)

Forma exacta que consume el front (`packages/shared-catalog`: `catalog.models.ts`,
`catalog.service.ts`, `product.service.ts`). Sustituye la sección Catalog de `docs/api-contract.md`
mediante la ADR 0008. Errores en [errors.md](errors.md).

Convenciones:
- Base `/api/v1/catalog` (constante `CatalogPaths.BASE`). Solo `GET`. JSON UTF-8.
- Toda respuesta (éxito o error) lleva la cabecera `X-Trace-Id`.
- `price` es número decimal (`5700.00`), precio "desde" del producto (mínimo de sus variantes).
- `image` es siempre una URL absoluta del CDN `https://blueprint.the-tractor.store/cdn/img/...`.
- `variants` es **una lista de SKUs** (strings), no objetos.
- Ninguna respuesta incluye `stock`, `enginePower` ni colores.
- Sin estado: no lee ni crea la cookie `ts_session`.

## Tipos

```ts
TeaserResponse        { id: string; title: string; image: string; filter: 'classic' | 'autonomous' }
ProductItemResponse   { id: string; name: string; brand: 'Classic' | 'Autonomous'; price: number;
                        image: string; variants: string[]; description: string }
CategoryResponse      { category: 'all' | 'classic' | 'autonomous'; products: ProductItemResponse[];
                        availableFilters: ['all', 'classic', 'autonomous'] }
ProductDetailResponse { id: string; name: string; brand: 'Classic' | 'Autonomous'; price: number;
                        image: string; description: string; variants: string[]; highlights: string[] }
StoreResponse         { id: string; name: string; address: string; city: string; image: string }
```

Correspondencia con el front: `TeaserResponse` = `CategoryTeaser`; `ProductItemResponse` =
`Tractor`/`ProductItemDto` (sin los opcionales `enginePower`, `stock`); `CategoryResponse` =
`CategoryData`; `ProductDetailResponse` = `ProductDetail`; `StoreResponse` = `StoreLocation`.

## GET /api/v1/catalog/home

**200** — array de 2 teasers, `classic` primero (el front reordena igualmente).

```json
[
  { "id": "classic", "title": "Classic Tractors",
    "image": "https://blueprint.the-tractor.store/cdn/img/scene/500/classics.webp", "filter": "classic" },
  { "id": "autonomous", "title": "Autonomous Tractors",
    "image": "https://blueprint.the-tractor.store/cdn/img/scene/500/autonomous.webp", "filter": "autonomous" }
]
```

## GET /api/v1/catalog/categories/{filter}

| Parámetro | Regla |
|---|---|
| `filter` (path) | exactamente `all`, `classic` o `autonomous` (sensible a mayúsculas); `@Size(max = 20)` |

**200** — `CategoryResponse`. `all` → 23 productos; `classic` → 15 (`CL-01`…`CL-15`);
`autonomous` → 8 (`AU-01`…`AU-08`). Orden del dataset.

```json
{
  "category": "classic",
  "products": [
    { "id": "CL-01", "name": "Heritage Workhorse", "brand": "Classic", "price": 5700.00,
      "image": "https://blueprint.the-tractor.store/cdn/img/product/400/CL-01-GR.webp",
      "variants": ["CL-01-GR", "CL-01-GY"],
      "description": "Proven reliability with a touch of modern reliability enhancements. Robust construction equipped to withstand decades of labor. User-friendly operation with traditional manual controls" }
  ],
  "availableFilters": ["all", "classic", "autonomous"]
}
```

| Caso | Respuesta |
|---|---|
| `filter` desconocido (`Classic`, `classics`, `tractors`) | 404 `CATEGORY_NOT_FOUND` |
| `filter` de más de 20 caracteres | 400 `VALIDATION_ERROR`, `details.filter` |

## GET /api/v1/catalog/products/{id}

| Parámetro | Regla |
|---|---|
| `id` (path) | `@Size(max = 20)` |

**200** — `ProductDetailResponse`. `variants` en orden del dataset; `highlights` tal cual.

```json
{
  "id": "AU-07", "name": "Verde Voyager", "brand": "Autonomous", "price": 4000.00,
  "image": "https://blueprint.the-tractor.store/cdn/img/product/400/AU-07-MT.webp",
  "description": "Adaptive drive system intelligently navigates through diverse field conditions. Clean energy operation with advanced solar battery technology. High-resolution field scanners for precise agronomy insights",
  "variants": ["AU-07-MT", "AU-07-YE"],
  "highlights": [
    "Adaptive drive system intelligently navigates through diverse field conditions",
    "Clean energy operation with advanced solar battery technology",
    "High-resolution field scanners for precise agronomy insights"
  ]
}
```

| Caso | Respuesta |
|---|---|
| `id` inexistente (`CL-99`) | 404 `PRODUCT_NOT_FOUND` |
| `id` de más de 20 caracteres | 400 `VALIDATION_ERROR`, `details.id` |

> Limitación conocida (research R2): `AU-07-YE` cuesta 5000 pero el detalle muestra 4000 ("desde").

## GET /api/v1/catalog/recommendations?skus=a,b

| Parámetro | Regla |
|---|---|
| `skus` (query, opcional) | lista separada por comas; sin constraints (nunca falla) |

**200** — array de hasta 4 `ProductItemResponse`. **`variants[0]` es el SKU recomendado**; el resto
de SKUs del producto sigue en orden del dataset.

- Con SKUs conocidos: orden por distancia RGB al color promedio, empate por SKU; sin los SKUs
  recibidos; un producto como mucho una vez; la categoría no interviene.
- Sin `skus`, `skus=`, `skus=,,` o solo SKUs desconocidos: **lista por defecto** →
  `CL-01` (`CL-01-GR`), `CL-02` (`CL-02-BL`), `CL-03` (`CL-03-GR`), `CL-04` (`CL-04-BL`).
- Nunca devuelve 4xx (solo 500 ante fallo inesperado).

Ejemplo verificado con el dataset (`?skus=AU-01-SI`, plata `#C0C0C0`): el más cercano es
autónomo y los siguientes son clásicos, lo que demuestra que la categoría no filtra.

```json
[
  { "id": "AU-07", "variants": ["AU-07-MT", "AU-07-YE"], "...": "..." },
  { "id": "CL-15", "variants": ["CL-15-SD", "CL-15-PI"], "...": "..." },
  { "id": "CL-08", "variants": ["CL-08-GR", "CL-08-PI"], "...": "..." },
  { "id": "CL-11", "variants": ["CL-11-SK", "CL-11-YE"], "...": "..." }
]
```

Otros casos de referencia para los tests (calculados con el dataset):

| `skus` | Productos (SKU recomendado) |
|---|---|
| `CL-01-GR` | `CL-09` (`CL-09-GR`), `CL-14` (`CL-14-GR`), `CL-07` (`CL-07-GR`), `CL-04` (`CL-04-RD`) |
| `AU-02-OG,AU-02-BL,AU-02-GG` | `AU-04` (`AU-04-BK`), `CL-09` (`CL-09-GR`), `CL-01` (`CL-01-GR`), `AU-06` (`AU-06-MT`) |
| `CL-02-BL,AU-05-ZZ` (el 2.º no existe) | `CL-13` (`CL-13-BL`), `AU-03` (`AU-03-TQ`), `CL-09` (`CL-09-BL`), `CL-04` (`CL-04-TQ`) |

## GET /api/v1/catalog/stores

**200** — array de 4 `StoreResponse`, orden del dataset.

```json
[
  { "id": "store-a", "name": "Aurora Flagship Store", "address": "Astronaut Way 1", "city": "Arlington",
    "image": "https://blueprint.the-tractor.store/cdn/img/store/400/store-1.webp" },
  { "id": "store-b", "name": "Big Micro Machines", "address": "Broadway 2", "city": "Burlington",
    "image": "https://blueprint.the-tractor.store/cdn/img/store/400/store-2.webp" },
  { "id": "store-c", "name": "Central Mall", "address": "Clown Street 3", "city": "Cryo",
    "image": "https://blueprint.the-tractor.store/cdn/img/store/400/store-3.webp" },
  { "id": "store-d", "name": "Downtown Model Store", "address": "Duck Street 4", "city": "Davenport",
    "image": "https://blueprint.the-tractor.store/cdn/img/store/400/store-4.webp" }
]
```

## OpenAPI (FR-026)

`/v3/api-docs` contiene exactamente estos 5 paths, cada uno con `summary`, `description` y
respuestas `200` + errores posibles (`400`, `404`, `500`) con esquema `ApiError`, bajo el tag
`Catalog`.
