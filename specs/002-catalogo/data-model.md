# Data model — 002 catálogo

Tres vistas del mismo catálogo: **dominio** (Java puro, reglas), **BD** (tablas `catalog_*` creadas
por `V2`), **JPA** (entidades de solo lectura que mapean las tablas). La forma HTTP está en
[contracts/catalog-api.md](contracts/catalog-api.md). Decisiones en [research.md](research.md).

## 1. Dominio (`com.tractorstore.catalog.domain`)

Todos son inmutables (records o clases finales); validan sus invariantes en el constructor y lanzan
`IllegalArgumentException` si se violan (son errores de programación o de datos, no de negocio).

### Color
| Campo | Tipo | Regla |
|---|---|---|
| `red`, `green`, `blue` | `double` | 0 ≤ valor ≤ 255 (`double` porque el promedio no es entero) |

- `static Color fromHex(String hex)`: `#RRGGBB`, sin distinguir mayúsculas. Formato inválido → excepción.
- `double distanceTo(Color other)`: euclidiana `√(Δr² + Δg² + Δb²)`.
- `static Color average(Collection<Color> colors)`: media por canal; colección vacía → excepción.

### Variant
| Campo | Tipo | Regla |
|---|---|---|
| `sku` | `String` | no vacío; único en el catálogo |
| `name` | `String` | no vacío (p. ej. `"Silver"`) |
| `color` | `Color` | |
| `price` | `BigDecimal` | > 0, escala 2 |
| `image` | `String` | URL absoluta |

### Category
| Campo | Tipo | Regla |
|---|---|---|
| `code` | `String` | `classic` \| `autonomous` |
| `brandLabel` | `String` | `"Classic"` \| `"Autonomous"` (FR-007) |
| `teaserTitle` | `String` | del dataset (`"Classic Tractors"`) |
| `teaserImage` | `String` | URL absoluta (escena 500) |

### CategoryFilter
Valor del path de listados. `static CategoryFilter of(String raw)`: `"all"` → `ALL` (sin categoría);
cualquier otro valor se trata como código de categoría y se resuelve contra `CategoryRepository`
en el caso de uso; si no existe → `CategoryNotFoundException`. Comparación **exacta** (sensible a
mayúsculas): `Classic`, `classics` → 404.

### Product
| Campo | Tipo | Regla |
|---|---|---|
| `id` | `String` | no vacío (`CL-01`) |
| `name` | `String` | no vacío |
| `category` | `Category` | |
| `highlights` | `List<String>` | orden del dataset; copia inmutable |
| `variants` | `List<Variant>` | ≥ 1; orden del dataset; copia inmutable |

Derivados (no se guardan):
- `BigDecimal startPrice()` = mínimo de `variants[].price` (FR-003).
- `String mainImage()` = `variants[0].image`.
- `List<String> skus()` = SKUs en orden del dataset.
- `String brand()` = `category.brandLabel()` (FR-007).
- `String description()` = highlights sin un `.` final, unidos con `". "` (FR-007 + research R4).
- `List<String> skusStartingWith(String sku)`: el SKU dado primero, el resto en orden (para
  recomendaciones).

### Store
| Campo | Tipo |
|---|---|
| `id` | `String` (`store-a`) |
| `name` | `String` |
| `address` | `String` (en el blueprint, `street`) |
| `city` | `String` |
| `image` | `String` (URL absoluta) |

### Recommendation
`record Recommendation(Product product, Variant variant)`: producto recomendado y su variante más
cercana.

### ColorRecommendationPolicy (servicio de dominio)
`List<Recommendation> recommend(List<Product> catalog, Collection<String> requestedSkus)`, con
`LIMIT = 4`. Reglas (FR-010 – FR-013, research R6):
1. Normaliza: `trim`, quita vacíos, quita duplicados, quita SKUs que no están en `catalog`.
2. Si no queda ninguno → **lista por defecto**: `(product, variants[0])` de los 4 primeros
   productos de `catalog` (que llega ordenado por `position`).
3. Si no: `target = Color.average(colores de los SKUs conocidos)`; candidatas = variantes cuyo SKU no
   está entre los recibidos; orden por `(distanceTo(target), sku)`; se queda con la primera de cada
   producto; corta en 4.
4. Nunca lanza excepciones de negocio. La categoría no interviene.

### Excepciones (extienden `shared.error.NotFoundException`)
| Clase | `code` | Mensaje |
|---|---|---|
| `ProductNotFoundException` | `PRODUCT_NOT_FOUND` | `Product <id> not found` |
| `CategoryNotFoundException` | `CATEGORY_NOT_FOUND` | `Category <filter> not found` |
| `VariantNotFoundException` | `VARIANT_NOT_FOUND` | `Variant <sku> not found` (sin uso en esta spec) |

### Puertos de salida (`domain.port`)
```text
ProductRepository   List<Product> findAll()                      // orden position
                    List<Product> findByCategory(String code)    // orden position
                    Optional<Product> findById(String id)
CategoryRepository  List<Category> findAll()                     // orden position
                    Optional<Category> findByCode(String code)
StoreRepository     List<Store> findAll()                        // orden position
```

## 2. Shared (`com.tractorstore.shared.error`, Java puro)

| Tipo | Contenido |
|---|---|
| `ErrorKind` (enum) | `NOT_FOUND`, `CONFLICT` (el segundo lo usarán carrito/pedidos) |
| `BusinessException` (abstract, `RuntimeException`) | `String code()`, `ErrorKind kind()` |
| `NotFoundException` (abstract) | `kind() = NOT_FOUND` |

## 3. Casos de uso (`com.tractorstore.catalog.application`)

| Puerto de entrada | Devuelve | Errores |
|---|---|---|
| `GetHomeTeasers.get()` | `List<Category>` | — |
| `GetCategoryListing.get(String filter)` | `CategoryListing(String filter, List<Product> products)` | `CategoryNotFoundException` |
| `GetProductDetail.get(String id)` | `Product` | `ProductNotFoundException` |
| `GetRecommendations.get(List<String> skus)` | `List<Recommendation>` (`skus` puede ser `null`) | — |
| `ListStores.list()` | `List<Store>` | — |

Las implementaciones (`*Service`) son `@Service` + `@Transactional(readOnly = true)`.

## 4. Base de datos (`V2__catalog_schema.sql`)

Sin claves foráneas fuera de `catalog_*`. `position` es `INTEGER`; en las tablas hijas empieza en 0
y es contigua (la exige `@OrderColumn`).

```text
catalog_category
  code           VARCHAR(20)  PK
  brand_label    VARCHAR(50)  NOT NULL
  teaser_title   VARCHAR(100) NOT NULL
  teaser_image   VARCHAR(255) NOT NULL
  position       INTEGER      NOT NULL UNIQUE

catalog_product
  id             VARCHAR(20)  PK
  name           VARCHAR(100) NOT NULL
  category_code  VARCHAR(20)  NOT NULL FK → catalog_category(code)
  position       INTEGER      NOT NULL UNIQUE
  INDEX (category_code)

catalog_product_highlight
  product_id     VARCHAR(20)  NOT NULL FK → catalog_product(id)
  position       INTEGER      NOT NULL CHECK (position >= 0)
  text           VARCHAR(255) NOT NULL
  PK (product_id, position)

catalog_variant
  sku            VARCHAR(20)   PK
  product_id     VARCHAR(20)   NOT NULL FK → catalog_product(id)
  position       INTEGER       NOT NULL CHECK (position >= 0)
  name           VARCHAR(100)  NOT NULL
  color          VARCHAR(7)    NOT NULL CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
  price          NUMERIC(10,2) NOT NULL CHECK (price > 0)
  image          VARCHAR(255)  NOT NULL
  UNIQUE (product_id, position)

catalog_store
  id             VARCHAR(20)  PK
  name           VARCHAR(100) NOT NULL
  address        VARCHAR(150) NOT NULL
  city           VARCHAR(100) NOT NULL
  image          VARCHAR(255) NOT NULL
  position       INTEGER      NOT NULL UNIQUE
```

### Dataset (`V3__catalog_dataset.sql`)
| Tabla | Filas | Origen |
|---|---|---|
| `catalog_category` | 2 | `classic` (`Classic`, `Classic Tractors`, `scene/500/classics.webp`), `autonomous` (`Autonomous`, `Autonomous Tractors`, `scene/500/autonomous.webp`) |
| `catalog_product` | 23 | `decide.products`; `position` = orden de `explore.categories` (`CL-01`=0 … `CL-15`=14, `AU-01`=15 … `AU-08`=22) |
| `catalog_product_highlight` | 69 | `decide.products[].highlights` (`highlightsa` en `AU-01`) |
| `catalog_variant` | 47 | `decide.products[].variants[]`; `image` = `https://blueprint.the-tractor.store/cdn/img/product/400/<SKU>.webp` |
| `catalog_store` | 4 | `explore.stores` (`street` → `address`); `image` = `.../cdn/img/store/400/store-N.webp` |

Idempotencia: Flyway aplica cada versión una sola vez (tabla `flyway_schema_history`); un segundo
arranque no ejecuta nada (US5-2). Una corrección futura del dataset va en una `V4`, nunca editando
`V3`.

## 5. JPA (`com.tractorstore.catalog.infrastructure.persistence`)

Todas `@Entity @Immutable @Table(name = "catalog_…")`, con constructor protegido sin argumentos y
solo getters. No salen del paquete: los adaptadores las mapean a dominio.

| Entidad | Mapeo |
|---|---|
| `CategoryEntity` | `@Id code`; `brandLabel`, `teaserTitle`, `teaserImage`, `position` |
| `ProductEntity` | `@Id id`; `name`; `position`; `@ManyToOne(fetch = LAZY) @JoinColumn(name = "category_code") CategoryEntity category`; `@ElementCollection @CollectionTable(name = "catalog_product_highlight", joinColumns = @JoinColumn(name = "product_id")) @OrderColumn(name = "position") @Column(name = "text") List<String> highlights`; `@OneToMany @JoinColumn(name = "product_id") @OrderColumn(name = "position") List<VariantEntity> variants` |
| `VariantEntity` | `@Id sku`; `name`; `color`; `@Column(precision = 10, scale = 2) BigDecimal price`; `image` (sin referencia al producto) |
| `StoreEntity` | `@Id id`; `name`, `address`, `city`, `image`, `position` |

Repositorios Spring Data (`JpaRepository<…>`, solo en este paquete):
- `JpaProductRepository`: `findAllByOrderByPositionAsc()`,
  `findAllByCategoryCodeOrderByPositionAsc(String)`, `findWithDetailsById(String)`, los tres con
  `@EntityGraph(attributePaths = {"category", "variants", "highlights"})` → 1 sentencia SQL.
- `JpaCategoryRepository`: `findAllByOrderByPositionAsc()`, `findById`.
- `JpaStoreRepository`: `findAllByOrderByPositionAsc()`.

## 6. Mapeo dominio → HTTP (`infrastructure.web.dto.CatalogDtoMapper`)

| DTO (record) | Desde | Campos |
|---|---|---|
| `TeaserResponse` | `Category` | `id = code`, `title = teaserTitle`, `image = teaserImage`, `filter = code` |
| `ProductItemResponse` | `Product` | `id`, `name`, `brand()`, `price = startPrice()`, `image = mainImage()`, `variants = skus()`, `description()` |
| `ProductItemResponse` (recomendación) | `Recommendation` | igual, con `variants = product.skusStartingWith(variant.sku())` |
| `CategoryResponse` | `CategoryListing` | `category = filter`, `products = [ProductItemResponse]`, `availableFilters = ["all","classic","autonomous"]` |
| `ProductDetailResponse` | `Product` | `id`, `name`, `brand()`, `price = startPrice()`, `image = mainImage()`, `description()`, `variants = skus()`, `highlights` |
| `StoreResponse` | `Store` | `id`, `name`, `address`, `city`, `image` |

Ningún DTO tiene `stock` ni `enginePower` (FR-008).
