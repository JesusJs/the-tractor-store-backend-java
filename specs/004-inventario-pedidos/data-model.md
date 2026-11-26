# Data model — 004 inventario y pedidos

Modelo por módulo y capa, siguiendo la skill `hexagonal-architecture`. Las decisiones están en
[research.md](research.md); las formas HTTP en [contracts/](contracts/).

---

## 1. Shared (`com.tractorstore.shared`)

### `error.ConflictException` (Java puro, nueva)
Clase abstracta. Extiende `BusinessException` con `kind() = ErrorKind.CONFLICT` → 409.

### `infrastructure.web.SessionCookies` (nueva, `@Component` público)
| Método | Comportamiento |
|---|---|
| `Optional<UUID> read(HttpServletRequest)` | Toma la primera cookie `ts_session`. Devuelve un valor solo si es un UUID canónico (R2). |
| `UUID resolveOrIssue(HttpServletRequest, HttpServletResponse)` | Si la cookie es válida, la devuelve sin `Set-Cookie`. Si no, genera un UUID aleatorio y añade `Set-Cookie` con `Path=/; HttpOnly; SameSite=Lax` (+ `Secure`). |

Constantes: `COOKIE_NAME = "ts_session"`.

### `infrastructure.web.SessionCookieProperties` (nueva)
`@ConfigurationProperties("tractorstore.session") record SessionCookieProperties(boolean secureCookie)`.
Vale `false` en `application.yml` y `true` en `application-prod.yml` (lo comprueba `SessionCookieProfileTests`, review del PR #5, A03).

### `infrastructure.web.JsonCoercionConfiguration` (nueva, `@Configuration` pública)
Bean `JsonMapperBuilderCustomizer` que hace fallar la coerción de número entero, decimal o booleano JSON a un campo de texto (`"variantId": 12` → 400 `MALFORMED_REQUEST`, FR-017). La dirección contraria y decimal → entero se desactivan con `spring.jackson.*` en `application.yml`. Registrada en el principio II de la constitución 1.3.0 y en la ADR 0011 (review del PR #5, A08).

### `infrastructure.web.InputPatterns` (nueva, clase de constantes pública)
`CATALOG_ID = "[A-Za-z0-9-]*"` y su mensaje; lo usan `@Pattern` del `{sku}` de `InventoryController` y los ids de `PlaceOrderRequest`/`OrderItemRequest` (review del PR #5, A06).

---

## 2. Inventory (`com.tractorstore.inventory`)

### Dominio
| Tipo | Campos | Invariantes / comportamiento |
|---|---|---|
| `model.StockItem` (record) | `String sku`, `int units` | `sku` no vacío, ≤ 20; `units ≥ 0`. `boolean canServe(int quantity)` (`quantity ≥ 1 && units ≥ quantity`). |
| `exception.StockNotFoundException` | — | `NotFoundException`, `VARIANT_NOT_FOUND`, "Variant {sku} not found" |
| `port.out.StockRepository` | — | `Optional<StockItem> findBySku(String sku)`; `boolean decreaseIfAvailable(String sku, int quantity)` (UPDATE condicional atómico: `true` si afectó 1 fila) |

### Aplicación
| Puerto de entrada | Servicio | Transacción |
|---|---|---|
| `GetStockUseCase.get(String sku) → StockItem` | `GetStockService` (lanza `StockNotFoundException`) | `readOnly = true` |
| `DecreaseStockUseCase.decrease(String sku, int quantity) → boolean` | `DecreaseStockService` (rechaza `quantity < 1` con `IllegalArgumentException`) | `MANDATORY` (se une a la de `order`; sin transacción activa lanza `IllegalTransactionStateException`, review #3) |
| — | `InventoryApiAdapter implements api.InventoryApi` → delega en `DecreaseStockUseCase` | — |

### API pública
`InventoryApi.decreaseStock(String sku, int quantity) → boolean` ([contracts/module-apis.md](contracts/module-apis.md)).

### Persistencia
`StockEntity` (`@Entity @Table(name = "inventory_stock")`): `@Id String sku`, `int stock`.

`JpaStockRepository extends JpaRepository<StockEntity, String>`:
```java
@Modifying
@Query("update StockEntity s set s.stock = s.stock - :quantity "
     + "where s.sku = :sku and s.stock >= :quantity")
int decreaseIfAvailable(String sku, int quantity);
```
`StockRepositoryAdapter` mapea `StockEntity` ↔ `StockItem` y convierte `int` en `== 1`.

---

## 3. Cart (`com.tractorstore.cart`)

| Capa | Tipo | Nota |
|---|---|---|
| domain | `model.Cart` (record `int totalItems, BigDecimal subTotal, tax, total`) | `static Cart empty()` con todo a 0. No hay líneas: nunca hay carrito en servidor (ADR 0011). |
| application | `GetCartUseCase.get() → Cart`; `GetCartService` | Sin transacción: no toca la BD. |
| web | `CartController` (`GET /api/v1/cart`): `sessionCookies.resolveOrIssue(...)` y después el caso de uso | DTO `CartResponse(List<CartItemResponse> items, int totalItems, BigDecimal subTotal, tax, total)`; `items` siempre `List.of()`. `CartItemResponse` solo existe para documentar la forma en OpenAPI. |

Sin persistencia ni tablas.

---

## 4. Catalog (`com.tractorstore.catalog`): ampliaciones

| Capa | Cambio |
|---|---|
| api | `CatalogApi` (`findVariants`, `storeExists`) y `VariantView` (R7) |
| domain.port.out | `ProductRepository.findByVariantSkus(Collection<String>) → List<Product>`; `StoreRepository.existsById(String) → boolean` |
| application.port.in | `FindVariantsUseCase.find(Collection<String> skus) → List<VariantView>`; `StoreExistsUseCase.exists(String id) → boolean` |
| application.service | `FindVariantsService`, `StoreExistsService` (`readOnly`), `CatalogApiAdapter implements CatalogApi` |
| persistence | `JpaProductRepository.findAllContainingSkus` (`@Query` + `exists` + `@EntityGraph`, R8); adaptadores ampliados |

Mapeo de `VariantView`: `sku` = `variant.sku()`, `productId` = `product.id()`,
`productName` = `product.name()`, `variantName` = `variant.name()`, `price` = `variant.price()`
(real, no el "desde") e `image` = `variant.image()`.

---

## 5. Order (`com.tractorstore.order`)

### Dominio: valores
| Tipo | Campos | Reglas |
|---|---|---|
| `model.OrderId` (record) | `UUID value` | `static OrderId newId()`; `static Optional<OrderId> parse(String)`: solo un UUID canónico (misma regex que R2), y cualquier otro valor → vacío; `toString()` = UUID en minúsculas. |
| `model.Money` (record) | `BigDecimal amount` | Escala 2 (`HALF_UP` si hay que ajustar); `amount ≥ 0`; `plus(Money)`, `times(int)`, `ZERO`. |
| `model.Vat` (final, sin estado) | `RATE = 0.21` | `static Money taxOn(Money subTotal)` = `subTotal × 0.21`, escala 2, `HALF_UP`. |
| `model.OrderStatus` (enum) | `PLACED` | Único estado en esta spec. |

### Dominio: agregado
**`model.OrderLine`** (record): `String sku`, `String productId`, `String productName`,
`String variantName`, `Money unitPrice`, `int quantity`, `String image`.
- Invariantes: textos no vacíos; `unitPrice > 0` (coherente con `CHECK (unit_price > 0)` de
  `order_line`: un precio 0 falla en el dominio y no como 500 de la BD); `1 ≤ quantity ≤ 99`.
- `Money lineTotal()` = `unitPrice × quantity`.
- `variantName` guarda el **nombre** de la variante del catálogo (copia histórica, FR-021). El
  recibo expone `variantName = sku` (FR-009, regresión .NET), así que lo resuelve el mapper HTTP y
  no el dominio.

**`model.Order`** (clase inmutable):

| Campo | Tipo | Nota |
|---|---|---|
| `id` | `OrderId` | |
| `sessionId` | `UUID` | de `ts_session` |
| `firstName`, `lastName`, `storeId` | `String` | tal cual llegan (no vacíos) |
| `extraPickups` | `String` (nullable) | tal cual llega |
| `lines` | `List<OrderLine>` | no vacía, copia inmutable, en el orden del cuerpo |
| `subTotal`, `tax`, `total` | `Money` | calculados en `place` |
| `status` | `OrderStatus` | `PLACED` |
| `placedAt` | `Instant` | UTC, truncado a milisegundos (R10) |

Comportamiento:
- `static Order place(OrderId, UUID sessionId, String firstName, String lastName, String storeId,
  String extraPickups, List<OrderLine> lines, Instant placedAt)`. Hace
  `subTotal = Σ lineTotal`, `tax = Vat.taxOn(subTotal)` y `total = subTotal + tax`, con
  `status = PLACED`.
- `static Order restore(...)`: reconstruye el pedido desde la persistencia con los importes
  guardados (no los recalcula).
- `List<String> extraPickupList()`: `null` o en blanco → `List.of()`. Si no, parte por `", "`
  literal y **descarta los fragmentos vacíos** (`"a, "` → `["a"]`). Nunca devuelve `null`.
- `boolean belongsTo(UUID sessionId)`.

### Dominio: excepciones (`domain.exception`)
`CartEmptyException` (409 `CART_EMPTY`), `StoreNotFoundException(storeId)` (404 `STORE_NOT_FOUND`),
`VariantNotFoundException(sku)` (404 `VARIANT_NOT_FOUND`), `OutOfStockException(sku)` (409
`OUT_OF_STOCK`), `OrderNotFoundException()` (404 `ORDER_NOT_FOUND`, mensaje fijo "Order not
found"). Ninguna revela si el pedido existe en otra sesión.

### Dominio: puertos de salida (`domain.port.out`)
```java
interface OrderRepository {
  void save(Order order);
  Optional<Order> findByIdAndSession(OrderId id, UUID sessionId);
}
interface OrderEventsGateway {          // [P3]
  void orderPlaced(Order order);
}
```

### Aplicación
- `model.PlaceOrderCommand` (record): `UUID sessionId, String firstName, String lastName,
  String storeId, String extraPickups, List<Line> lines`, con `record Line(String sku, int quantity)`.
  `lines` puede venir `null` o vacía (→ `CART_EMPTY`).
- `port.in.PlaceOrderUseCase.place(PlaceOrderCommand) → Order`. Implementación
  `PlaceOrderService` (`@Transactional`). Depende de `CatalogApi`, `InventoryApi`,
  `OrderRepository`, `java.time.Clock` y, en P3, `OrderEventsGateway`. Algoritmo en R9.
- `port.in.GetOrderUseCase.get(String orderId, Optional<UUID> sessionId) → Order`. Implementación
  `GetOrderService` (`readOnly`). Un id inválido, la ausencia de sesión o una búsqueda vacía lanzan
  `OrderNotFoundException`.

### Persistencia
`OrderEntity` (`@Entity @Table(name = "order_order")`): campos escalares 1:1 con la tabla, más
```java
@ElementCollection
@CollectionTable(name = "order_line", joinColumns = @JoinColumn(name = "order_id"))
@OrderColumn(name = "position")
private List<OrderLineEmbeddable> lines;
```
`OrderLineEmbeddable` (`@Embeddable`): `sku`, `productId`, `productName`, `variantName`,
`unitPrice` (`@Column(precision = 10, scale = 2)`), `quantity` e `image`.

`JpaOrderRepository extends JpaRepository<OrderEntity, UUID>`:
`@EntityGraph(attributePaths = "lines") Optional<OrderEntity> findByIdAndSessionId(UUID id, UUID sessionId)`.

`OrderRepositoryAdapter` mapea en los dos sentidos con `Order.restore`.

### Infraestructura: evento [P3]
`SpringOrderEventsGateway` (`infrastructure.event`, `@Component`) convierte `Order` en
`OrderPlaced(orderId, sessionId, lines[sku, quantity])` y llama a
`ApplicationEventPublisher.publishEvent`.

### Infraestructura: config
`OrderClockConfiguration`: `@Bean Clock orderClock()` → `Clock.systemUTC()`.

---

## 6. Notifications (`com.tractorstore.notifications`) [P3]

| Capa | Tipo |
|---|---|
| application | `NotifyOrderPlacedUseCase.notify(UUID orderId)`; `NotifyOrderPlacedService` escribe `INFO "Confirmation email sent for order {orderId}"` |
| infrastructure.event | `OrderPlacedListener`: `@ApplicationModuleListener void on(OrderPlaced event)` → `notify(event.orderId())` |

Sin dominio propio ni tablas (no hay email real).

---

## 7. Base de datos

### `V4__inventory_stock.sql` (P1)
```text
inventory_stock
  sku    VARCHAR(20) PRIMARY KEY
  stock  INTEGER     NOT NULL CHECK (stock >= 0)
```
47 filas (todos los SKUs de `V3`): 10 por defecto, `CL-01-GY` = 0, `AU-08-WH` = 1 y
`AU-07-YE` = 5 (R1). Sin FK a `catalog_variant` (constitución IV).

### `V5__order_schema.sql` (P2)
```text
order_order
  id            UUID          PRIMARY KEY
  session_id    UUID          NOT NULL
  first_name    VARCHAR(100)  NOT NULL
  last_name     VARCHAR(100)  NOT NULL
  store_id      VARCHAR(20)   NOT NULL          -- sin FK a catalog_store (validado por catalog.api)
  extra_pickups VARCHAR(500)                    -- NULL si no llegó
  sub_total     NUMERIC(10,2) NOT NULL CHECK (sub_total >= 0)
  tax           NUMERIC(10,2) NOT NULL CHECK (tax >= 0)
  total         NUMERIC(10,2) NOT NULL CHECK (total >= 0)
  status        VARCHAR(20)   NOT NULL CHECK (status IN ('PLACED'))
  placed_at     TIMESTAMP WITH TIME ZONE NOT NULL

order_line
  order_id      UUID          NOT NULL REFERENCES order_order (id)   -- FK interna del módulo
  position      INTEGER       NOT NULL CHECK (position >= 0)
  sku           VARCHAR(20)   NOT NULL                               -- sin FK a catalog_variant
  product_id    VARCHAR(20)   NOT NULL
  product_name  VARCHAR(100)  NOT NULL
  variant_name  VARCHAR(100)  NOT NULL
  unit_price    NUMERIC(10,2) NOT NULL CHECK (unit_price > 0)
  quantity      INTEGER       NOT NULL CHECK (quantity BETWEEN 1 AND 99)
  image         VARCHAR(255)  NOT NULL
  PRIMARY KEY (order_id, position)
```
La búsqueda por `(id, session_id)` usa la PK; no hace falta un índice por `session_id` porque no
hay listados por sesión. Los tipos coinciden exactamente con el mapeo JPA para que pase `validate`
(`VARCHAR`, `INTEGER`, `UUID`, `NUMERIC(10,2)`, `timestamptz` ↔ `Instant`).

### `V6__event_publication_registry.sql` (P3)
DDL v2 literal de Modulith 2.1.1 (`event_publication` + 2 índices), copiado en
[research.md § R11](research.md#r11-event-publication-registry-ddl-exacto-de-modulith-211-fr-023--verificado).
Sin tabla de archivo.

---

## 8. Mapeos HTTP

### `OrderDtoMapper` (`order.infrastructure.web.mapper`)
| `OrderReceiptResponse` | Origen |
|---|---|
| `id` | `order.id().toString()` |
| `firstName`, `lastName`, `storeId` | tal cual |
| `extraPickups` | `order.extraPickupList()` (nunca `null`) |
| `items[]` | por línea: `productId`, `variantId` = `sku`, `productName`, **`variantName` = `sku`**, `price` = `unitPrice`, `quantity`, `image` |
| `subTotal`, `tax`, `total` | `Money.amount()` (escala 2) |
| `placedAt` | `Instant` → ISO-8601 UTC (`2026-11-24T21:15:03.123Z`) |
| `status` | `"PLACED"` |

`PlaceOrderRequest` → `PlaceOrderCommand`: copia `firstName`, `lastName`, `storeId` y
`extraPickups`, y convierte `items` (o `null`) en `Line(variantId, quantity)`.

### Inventario
`StockResponse(String sku, int stock)` ← `StockItem(sku, units)`.

---

## 9. Transiciones de estado

Solo `PLACED`, que se asigna al crear el pedido. No hay cancelación, pago ni recogida en esta spec.
`status` lleva un `CHECK` en BD y un enum de un valor para que añadir estados sea un cambio
explícito (migración nueva + enum).
