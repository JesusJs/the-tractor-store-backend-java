---

description: "Lista de tareas de 004 inventario y pedidos"
---

# Tasks: Inventario y pedidos (stock, sesión, pedido y evento `OrderPlaced`)

**Input**: Documentos de diseño de `specs/004-inventario-pedidos/`

**Prerequisites**: plan.md, spec.md, research.md (R1–R16), data-model.md, contracts/ (inventory-cart-api.md, orders-api.md, module-apis.md), quickstart.md

**Tests**: SÍ. La constitución III (pruebas primero) y el plan exigen test en rojo antes de cada
código. En cada historia las tareas de test van primero y **deben fallar** antes de implementar.

**Organization**: por historia de usuario (P1 → P2 → P3), de dentro hacia afuera (dominio →
aplicación → adaptadores). Entre US2 y US3 va la fase transversal de guardas de arquitectura
(FR-028..FR-030) porque P3 es lo primero que se recorta y las guardas no deben recortarse con él.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede hacer en paralelo (ficheros distintos, sin dependencias pendientes)
- **[Story]**: historia a la que pertenece (US1, US2, US3)
- Rutas relativas a la raíz del repo. Paquete raíz `com.tractorstore`:
  - `main/` = `src/main/java/com/tractorstore/`
  - `test/` = `src/test/java/com/tractorstore/`
  - `fixtures/` = `src/test/java/archfixtures/`
  - `res/` = `src/main/resources/`

**Convenciones que aplican a todas las tareas** (no se repiten en cada una):
- Cargar las skills `spring-boot-4-notes` (Jackson 3, `@MockitoBean`, Modulith 2), `hexagonal-architecture`,
  `modular-architecture`, `testing-strategy` y `api-contract` antes de tocar el código correspondiente.
- Dominio en Java puro (sin Spring/JPA/Jackson/`jakarta..`). En `application` solo `@Service` y `@Transactional`.
- Regresiones .NET con `@DisplayName("regresión: …")`.
- `./mvnw spotless:apply` antes de cada commit; commits en inglés (conventional commits, sin `Co-Authored-By`).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: dejar la rama lista. No hay dependencias nuevas en P1/P2 (`pom.xml` solo cambia en P3).

- [X] T001 Verificar la línea base: `docker compose up -d postgres` y `./mvnw verify` en verde en la rama `004-inventario-pedidos` antes de tocar código (anotar cualquier fallo previo; no se arregla aquí)
- [X] T002 [P] Añadir `tractorstore.session.secure-cookie: false` en `res/application.yml` y `tractorstore.session.secure-cookie: true` en `res/application-prod.yml` (R2; mismo estilo que el bloque de CORS existente)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 409 en el handler y componente de sesión `ts_session`, que usan US1 (cart) y US2 (orders).

**⚠️ CRITICAL**: ninguna historia empieza sin esta fase.

### Tests (rojo primero)

- [X] T003 [P] Ampliar `test/shared/infrastructure/web/GlobalExceptionHandlerTest.java`: una subclase de prueba de `ConflictException` con `code = "OUT_OF_STOCK"` → 409, cuerpo `{ code, message, traceId }` con el `code` de la excepción (R5)
- [X] T004 [P] Crear `test/shared/infrastructure/web/SessionCookiesTest.java` (sin Spring, `MockHttpServletRequest`/`MockHttpServletResponse`): `read` sin cookie → vacío; UUID canónico → valor; `"1-1-1-1-1"` y `"abc"` → vacío; dos cookies `ts_session` → usa la primera; `resolveOrIssue` con UUID válido → lo devuelve y **no** añade `Set-Cookie`; sin cookie o inválida → UUID nuevo **distinto** del recibido y `Set-Cookie` con `ts_session=<uuid>; Path=/; HttpOnly; SameSite=Lax`, sin `Max-Age`; con `secureCookie=true` añade `Secure` y con `false` no (R2)

### Implementación

- [X] T005 [P] Crear `main/shared/error/ConflictException.java`: clase abstracta, `extends BusinessException`, `kind() = ErrorKind.CONFLICT` (Java puro); actualizar el javadoc de `ErrorKind.CONFLICT` en `main/shared/error/ErrorKind.java` (quitar la mención a "spec 007")
- [X] T006 Añadir la rama `CONFLICT → 409` con el `code` de la excepción en `main/shared/infrastructure/web/GlobalExceptionHandler.java` y quitar el comentario "spec 007 adds the 409" (depende de T005; pasa T003)
- [X] T007 [P] Crear `main/shared/infrastructure/web/SessionCookieProperties.java`: `@ConfigurationProperties("tractorstore.session") record SessionCookieProperties(boolean secureCookie)` y registrarlo igual que `CorsProperties` (depende de T002)
- [X] T008 Crear `main/shared/infrastructure/web/SessionCookies.java` (`@Component` público): constante `COOKIE_NAME = "ts_session"`; `Optional<UUID> read(HttpServletRequest)` con la regex canónica `^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$` + `UUID.fromString`; `UUID resolveOrIssue(HttpServletRequest, HttpServletResponse)` que usa `ResponseCookie` (`Path=/`, `HttpOnly`, `SameSite=Lax`, `Secure` según propiedad, sin `Max-Age`) (depende de T007; pasa T004)
- [X] T009 Actualizar el comentario de `main/shared/package-info.java` para incluir la sesión por cookie `ts_session` (ADR 0011, pendiente del Sync Impact Report de la constitución 1.3.0)

**Checkpoint**: `./mvnw test -Dtest='GlobalExceptionHandlerTest,SessionCookiesTest'` en verde.

---

## Phase 3: User Story 1 - Ver el stock de una variante y obtener sesión (Priority: P1) 🎯 MVP

**Goal**: `GET /api/v1/inventory/{sku}` devuelve `{ sku, stock }` desde `inventory_stock` y `GET /api/v1/cart` devuelve el carrito vacío fijando `ts_session`.

**Independent Test**: con la BD migrada desde vacío, `curl /api/v1/inventory/CL-01-GR` → 200 `stock: 10`; `CL-01-GY` → 200 `stock: 0`; `XX-99-ZZ` → 404 `VARIANT_NOT_FOUND`; SKU de 21 caracteres → 400 `VALIDATION_ERROR`; `curl -i /api/v1/cart` sin cookie → 200 vacío + `Set-Cookie: ts_session=…` (quickstart §3). Con el front sin cambios la ficha muestra "N in stock"/"Out of stock".

### Tests for User Story 1 (rojo primero) ⚠️

- [X] T010 [P] [US1] Crear `test/inventory/domain/model/StockItemTest.java`: `sku` no vacío y ≤ 20; `units ≥ 0` (negativo → `IllegalArgumentException`); `canServe(q)` es `q ≥ 1 && units ≥ q` (casos 0, igual, mayor)
- [X] T011 [P] [US1] Crear `test/inventory/application/service/GetStockServiceTest.java` (puerto `StockRepository` mockeado): SKU existente → `StockItem`; ausente → `StockNotFoundException` con `code = VARIANT_NOT_FOUND`
- [X] T012 [P] [US1] Crear `test/inventory/application/service/DecreaseStockServiceTest.java` (delega en `decreaseIfAvailable` y devuelve su `boolean`; `quantity < 1` → `IllegalArgumentException` sin llamar al puerto) y `test/inventory/application/service/InventoryApiAdapterTest.java` (delega en `DecreaseStockUseCase` y devuelve su resultado)
- [X] T013 [P] [US1] Crear `test/inventory/infrastructure/persistence/adapter/StockPersistenceIT.java` (`@DataJpaTest` + Testcontainers `postgres:17.12`, BD migrada): `inventory_stock` tiene exactamente una fila por SKU de `catalog_variant` (47; el test puede cruzar tablas), `CL-01-GY` = 0, `AU-08-WH` = 1, `AU-07-YE` = 5, resto 10; `decreaseIfAvailable` con stock suficiente → `true` y baja; con stock insuficiente → `false` y no cambia; SKU sin fila → `false`; el `CHECK (stock >= 0)` existe. Fija sus valores en `@BeforeEach` con `JdbcTemplate` (R15)
- [X] T014 [P] [US1] Crear `test/inventory/infrastructure/web/controller/InventoryControllerTest.java` (`@WebMvcTest` + `@MockitoBean GetStockUseCase`): 200 `{ sku, stock }` por JSONPath; caso de uso lanza `StockNotFoundException` → 404 `VARIANT_NOT_FOUND` con `traceId`; SKU de 21 caracteres → 400 `VALIDATION_ERROR` sin llamar al caso de uso; la respuesta **no** lleva `Set-Cookie`
- [X] T015 [P] [US1] Crear `test/cart/infrastructure/web/controller/CartControllerTest.java` (`@WebMvcTest`, importa `SessionCookies` + `SessionCookieProperties`, `@MockitoBean GetCartUseCase`): 200 `{ items: [], totalItems: 0, subTotal: 0, tax: 0, total: 0 }`; sin cookie → `Set-Cookie ts_session` con UUID; cookie UUID válida → sin `Set-Cookie`; cookie `"no-uuid"` → `Set-Cookie` con un valor nuevo distinto (AS-1.5..1.7)
- [X] T016 [P] [US1] Crear `test/cart/domain/model/CartTest.java` (`Cart.empty()` → `totalItems = 0` y `subTotal`, `tax`, `total` = 0) y `test/cart/application/service/GetCartServiceTest.java` (sin Spring; `get()` devuelve un carrito igual a `Cart.empty()`) (FR-005)

### Implementation for User Story 1

- [X] T017 [P] [US1] Crear `main/inventory/domain/model/StockItem.java` (record `String sku, int units`, invariantes y `canServe`) y `main/inventory/domain/exception/StockNotFoundException.java` (`extends NotFoundException`, `code = "VARIANT_NOT_FOUND"`, mensaje `"Variant {sku} not found"`) (pasa T010)
- [X] T018 [P] [US1] Crear el puerto `main/inventory/domain/port/out/StockRepository.java`: `Optional<StockItem> findBySku(String sku)` y `boolean decreaseIfAvailable(String sku, int quantity)` (UPDATE condicional atómico; `true` si afectó 1 fila)
- [X] T019 [P] [US1] Crear `main/inventory/api/InventoryApi.java` con `boolean decreaseStock(String sku, int quantity)` y el javadoc literal de `contracts/module-apis.md` (sin excepciones de negocio; `IllegalArgumentException` si `quantity < 1`; necesita transacción del llamante)
- [X] T020 [US1] Crear `main/inventory/application/port/in/GetStockUseCase.java` (`StockItem get(String sku)`), `main/inventory/application/port/in/DecreaseStockUseCase.java` (`boolean decrease(String sku, int quantity)`), `main/inventory/application/service/GetStockService.java` (`@Transactional(readOnly = true)`, lanza `StockNotFoundException`), `main/inventory/application/service/DecreaseStockService.java` (`@Transactional` `MANDATORY` tras la review #3; `REQUIRED` en el plan original; rechaza `quantity < 1`) y `main/inventory/application/service/InventoryApiAdapter.java` (package-private `@Service implements InventoryApi`, delega en `DecreaseStockUseCase`) (depende de T017–T019; pasa T011, T012)
- [X] T021 [US1] Crear `res/db/migration/V4__inventory_stock.sql`: tabla `inventory_stock (sku VARCHAR(20) PRIMARY KEY, stock INTEGER NOT NULL CHECK (stock >= 0))`, sin FK a `catalog_variant`, e insertar una fila por cada uno de los 47 SKUs de `V3__catalog_dataset.sql` con 10 unidades salvo `CL-01-GY` = 0, `AU-08-WH` = 1 y `AU-07-YE` = 5 (R1). No tocar V1–V3
- [X] T022 [US1] Crear `main/inventory/infrastructure/persistence/entity/StockEntity.java` (`@Entity @Table(name = "inventory_stock")`, `@Id String sku`, `int stock`), `main/inventory/infrastructure/persistence/repository/JpaStockRepository.java` (`JpaRepository<StockEntity, String>` con `@Modifying @Query("update StockEntity s set s.stock = s.stock - :quantity where s.sku = :sku and s.stock >= :quantity") int decreaseIfAvailable(String sku, int quantity)`, **sin** `@Transactional`) y `main/inventory/infrastructure/persistence/adapter/StockRepositoryAdapter.java` (mapea a `StockItem`; `== 1` → `true`) (depende de T018, T021; pasa T013)
- [X] T023 [US1] Crear `main/inventory/infrastructure/web/controller/InventoryPaths.java`, `main/inventory/infrastructure/web/dto/StockResponse.java` (record `String sku, int stock`) y `main/inventory/infrastructure/web/controller/InventoryController.java` (`GET /api/v1/inventory/{sku}` con `@Size(max = 20)` en el `@PathVariable`, solo llama a `GetStockUseCase`, no usa `SessionCookies`; tag `Inventory` y `@Operation`/`@ApiResponse` 200, 400, 404, 500 según `contracts/inventory-cart-api.md`) (depende de T020; pasa T014)
- [X] T024 [P] [US1] Crear `main/cart/domain/model/Cart.java` (record `int totalItems, BigDecimal subTotal, BigDecimal tax, BigDecimal total`, `static Cart empty()` todo a 0), `main/cart/application/port/in/GetCartUseCase.java` (`Cart get()`) y `main/cart/application/service/GetCartService.java` (`@Service`, sin transacción) (pasa T016)
- [X] T025 [US1] Crear `main/cart/infrastructure/web/controller/CartPaths.java`, `main/cart/infrastructure/web/dto/CartResponse.java` (record `List<CartItemResponse> items, int totalItems, BigDecimal subTotal, BigDecimal tax, BigDecimal total`; `items` siempre `List.of()`), `main/cart/infrastructure/web/dto/CartItemResponse.java` (solo documenta la forma en OpenAPI) y `main/cart/infrastructure/web/controller/CartController.java` (`GET /api/v1/cart`: `sessionCookies.resolveOrIssue(...)` antes del caso de uso; tag `Cart`, `@Operation` documentando el `Set-Cookie`) (depende de T008, T024; pasa T015)
- [X] T026 [P] [US1] Crear `test/inventory/InventoryApiIT.java` (`@SpringBootTest(RANDOM_PORT)` + `RestTestClient`): 200 con stock > 0, 200 con `CL-01-GY` = 0, 404 `VARIANT_NOT_FOUND`, 400 por SKU > 20, sin `Set-Cookie` (AS-1.1..1.4)
- [X] T027 [P] [US1] Crear `test/cart/CartApiIT.java` (`RANDOM_PORT`): `Set-Cookie` real con `HttpOnly`, `SameSite=Lax`, `Path=/` y sin `Secure` en perfil test; segunda petición con esa cookie → sin `Set-Cookie`; cookie inválida → nueva (AS-1.5..1.7)

**Checkpoint (⏱ corte P1)**: `./mvnw verify` en verde; quickstart §3 con curl; con el front local la ficha muestra "N in stock"/"Out of stock" y el navegador recibe `ts_session` al abrir el carrito.

---

## Phase 4: User Story 2 - Confirmar el pedido y consultar el recibo (Priority: P2)

**Goal**: `POST /api/v1/orders` confirma con precio real del catálogo, IVA 21 % `HALF_UP`, descuento atómico de stock y todo-o-nada; `GET /api/v1/orders/{id}` devuelve el recibo solo a la misma sesión.

**Independent Test**: quickstart §4 (curl con el cuerpo exacto del front: 201 + `Location` + cookie, importes, stock descontado, 404 desde otra cookie, errores) y §5 (front real: ficha → carrito → checkout → gracias sin errores).

**Depende de**: Phase 2 (409 y `SessionCookies`) y de `InventoryApi` de US1 (T019, T020, T022).

### Tests for User Story 2 (rojo primero) ⚠️

- [X] T028 [P] [US2] Crear `test/order/domain/model/MoneyTest.java`: normaliza a escala 2 (`HALF_UP`), negativo → `IllegalArgumentException`, `plus`, `times(int)`, `ZERO`
- [X] T029 [P] [US2] Crear `test/order/domain/model/VatTest.java` con `@ParameterizedTest`: 0,05 → 0,01; 0,02 → 0,00; 2,50 → 0,53; 5000 → 1050,00; 43 065 000 → 9 043 650,00 (R10)
- [X] T030 [P] [US2] Crear `test/order/domain/model/OrderIdTest.java`: `newId()` distinto cada vez; `parse` acepta solo UUID canónico (misma regex que R2) y devuelve vacío para `"1-1-1-1-1"`, `"abc"`, `""` y `null`; `toString()` en minúsculas
- [X] T031 [P] [US2] Crear `test/order/domain/model/OrderTest.java`: `place` calcula `subTotal = Σ unitPrice × quantity`, `tax = Vat.taxOn(subTotal)`, `total = subTotal + tax`, `status = PLACED`; líneas no vacías y en el orden recibido (copia inmutable); `OrderLine` exige textos no vacíos, `unitPrice > 0` (0 → `IllegalArgumentException`, alineado con `CHECK (unit_price > 0)` de `V5`) y `1 ≤ quantity ≤ 99`; `extraPickupList()`: `null`/`""`/`"  "` → `[]`, `"Store A, Store B"` → `["Store A","Store B"]`, `"a, "` → `["a"]`; `belongsTo(session)`; `restore` no recalcula importes
- [X] T032 [P] [US2] Crear `test/catalog/application/service/FindVariantsServiceTest.java`, `test/catalog/application/service/StoreExistsServiceTest.java` (puertos mockeados) y `test/catalog/application/service/CatalogApiAdapterTest.java` (casos de uso mockeados; delega en ellos): `findVariants` devuelve una `VariantView` por SKU pedido existente con `price` = precio real de la variante (no el "desde"), `variantName` = nombre de la variante, `productId`/`productName` del producto e `image` de la variante; SKUs desconocidos se omiten; `storeExists` delega en `StoreRepository.existsById`
- [X] T033 [P] [US2] Ampliar `test/catalog/infrastructure/persistence/adapter/CatalogPersistenceIT.java`: `findByVariantSkus(["AU-07-YE","CL-01-GY"])` devuelve los productos con **todas** sus variantes cargadas y en **una sola** sentencia SQL (mismo patrón que FR-024 de 002); `existsById` → `true` para un id de tienda sembrado y `false` para `"store-x"` y para un id con distinta mayúscula (comparación exacta)
- [X] T034 [P] [US2] Crear `test/order/application/service/PlaceOrderServiceTest.java` (`CatalogApi`, `InventoryApi`, `OrderRepository` mockeados, `Clock.fixed`): precedencia `CART_EMPTY` (`lines` null y vacía) → `STORE_NOT_FOUND` → `VARIANT_NOT_FOUND` (primer SKU en orden de líneas) → `OUT_OF_STOCK`, sin llamar a `save` en ningún fallo; `findVariants` se llama **una** vez con los SKUs distintos; `decreaseStock` en orden de SKU (orden estable) y una llamada por línea aunque se repita el SKU; `regresión: precio enviado por el cliente` (la línea usa `price` de `VariantView`, 5000 para `AU-07-YE`); `placedAt` = `Instant.now(clock)` truncado a milisegundos; sesión del comando en el pedido; si `orders.save` lanza una excepción de persistencia (simulada con `RuntimeException`), el servicio **la propaga** sin capturarla, para que el proxy transaccional revierta los descuentos y el handler responda 500 `INTERNAL_ERROR` (edge case "BD caída")
- [X] T035 [P] [US2] Crear `test/order/application/service/GetOrderServiceTest.java`: id no UUID → `OrderNotFoundException` sin tocar el repositorio; sesión vacía → `OrderNotFoundException`; búsqueda vacía (otra sesión o inexistente) → `OrderNotFoundException` con mensaje fijo `"Order not found"`; caso feliz → el `Order`
- [X] T036 [P] [US2] Crear `test/order/infrastructure/persistence/adapter/OrderPersistenceIT.java` (`@DataJpaTest` + Testcontainers): guardar y releer con `findByIdAndSession` devuelve un `Order` igual (escala 2 en importes, `placedAt` con milisegundos idéntico, orden de líneas por `position`, `extraPickups` `null` se conserva); otra sesión → vacío
- [X] T037 [P] [US2] Crear `test/order/infrastructure/web/controller/OrderControllerTest.java` (`@WebMvcTest`, importa `SessionCookies`, `@MockitoBean` de los dos casos de uso): con el **cuerpo exacto** de `contracts/orders-api.md` (incluye `price`, `productId`, `productName`, `variantName`, `image`) → 201, `Location: /api/v1/orders/{id}`, `Set-Cookie` si no hay cookie, recibo con todos los campos por JSONPath, `items[0].variantName` = SKU, `extraPickups` como lista y `[]` cuando falta; `extraPickups` array → 400 `MALFORMED_REQUEST`; `quantity` texto → 400 `MALFORMED_REQUEST`; `variantId` `""` y de 21 caracteres, `quantity` 0 y 100, `firstName` en blanco o de 101 caracteres, `lastName` en blanco o de 101 caracteres, `storeId` en blanco o de 21, `extraPickups` de 501, 51 líneas y un elemento `null` en `items` → 400 `VALIDATION_ERROR` con la ruta del campo en `details` (p. ej. `items[0].variantId`) sin llamar al caso de uso; excepciones de dominio → 409 `CART_EMPTY`, 404 `STORE_NOT_FOUND`, 404 `VARIANT_NOT_FOUND`, 409 `OUT_OF_STOCK`; `GET` con cookie → 200; `GET` no crea cookie; `OrderNotFoundException` → 404 `{ code: ORDER_NOT_FOUND, message: "Order not found", traceId }`

### Implementation for User Story 2

- [X] T038 [P] [US2] Crear en `main/order/domain/model/` (dominio puro): `Money.java` (record `BigDecimal amount`, escala 2 `HALF_UP`, `amount ≥ 0`, `plus`, `times(int)`, `ZERO`), `Vat.java` (final, `RATE = new BigDecimal("0.21")`, `static Money taxOn(Money)` escala 2 `HALF_UP`), `OrderId.java` (record `UUID value`, `newId()`, `parse(String) → Optional<OrderId>` con regex canónica, `toString()` minúsculas) y `OrderStatus.java` (enum con solo `PLACED`) (pasa T028–T030)
- [X] T039 [US2] Crear `main/order/domain/model/OrderLine.java` (record `String sku, String productId, String productName, String variantName, Money unitPrice, int quantity, String image`; textos no vacíos, `unitPrice > 0`, `1 ≤ quantity ≤ 99`, `lineTotal()`) y `main/order/domain/model/Order.java` (inmutable; `place(...)`, `restore(...)`, `extraPickupList()` partiendo por `", "` literal y descartando vacíos, nunca `null`; `belongsTo(UUID)`) según data-model §5 (depende de T038; pasa T031)
- [X] T040 [P] [US2] Crear en `main/order/domain/exception/`: `CartEmptyException` (`ConflictException`, `CART_EMPTY`), `StoreNotFoundException(storeId)` (`NotFoundException`, `STORE_NOT_FOUND`), `VariantNotFoundException(sku)` (`NotFoundException`, `VARIANT_NOT_FOUND`), `OutOfStockException(sku)` (`ConflictException`, `OUT_OF_STOCK`) y `OrderNotFoundException()` (`NotFoundException`, `ORDER_NOT_FOUND`, mensaje fijo `"Order not found"`, sin repetir el id) (depende de T005)
- [X] T041 [P] [US2] Crear el puerto `main/order/domain/port/out/OrderRepository.java`: `void save(Order order)` y `Optional<Order> findByIdAndSession(OrderId id, UUID sessionId)`
- [X] T042 [P] [US2] Crear `main/catalog/api/CatalogApi.java` (`List<VariantView> findVariants(Collection<String> skus)`, `boolean storeExists(String storeId)`, javadoc de `contracts/module-apis.md`) y `main/catalog/api/VariantView.java` (record `String sku, String productId, String productName, String variantName, BigDecimal price, String image`); solo tipos del JDK
- [X] T043 [US2] Ampliar los puertos `main/catalog/domain/port/out/ProductRepository.java` (`List<Product> findByVariantSkus(Collection<String> skus)`) y `main/catalog/domain/port/out/StoreRepository.java` (`boolean existsById(String id)`); crear `main/catalog/application/port/in/FindVariantsUseCase.java`, `main/catalog/application/port/in/StoreExistsUseCase.java`, `main/catalog/application/service/FindVariantsService.java` y `main/catalog/application/service/StoreExistsService.java` (`@Transactional(readOnly = true)`, propagación `REQUIRED`) y `main/catalog/application/service/CatalogApiAdapter.java` (package-private `@Service implements CatalogApi`, delega en los dos casos de uso) (depende de T042; pasa T032)
- [X] T044 [US2] Añadir a `main/catalog/infrastructure/persistence/repository/JpaProductRepository.java` `@EntityGraph(attributePaths = {"category", "variants", "highlights"}) @Query("select p from ProductEntity p where exists (select v from p.variants v where v.sku in :skus)") List<ProductEntity> findAllContainingSkus(Collection<String> skus)` (R8; no añadir `@ManyToOne` a `VariantEntity`) e implementar los métodos nuevos en `main/catalog/infrastructure/persistence/adapter/ProductRepositoryAdapter.java` y `StoreRepositoryAdapter.java` (depende de T043; pasa T033)
- [X] T045 [US2] Crear `main/order/application/model/PlaceOrderCommand.java` (record `UUID sessionId, String firstName, String lastName, String storeId, String extraPickups, List<Line> lines` con `record Line(String sku, int quantity)`; `lines` puede ser `null`), `main/order/application/port/in/PlaceOrderUseCase.java` (`Order place(PlaceOrderCommand)`) y `main/order/application/port/in/GetOrderUseCase.java` (`Order get(String orderId, Optional<UUID> sessionId)`)
- [X] T046 [US2] Crear `main/order/application/service/PlaceOrderService.java` (`@Service`, un único método `@Transactional`; depende de `CatalogApi`, `InventoryApi`, `OrderRepository`, `java.time.Clock`) con el algoritmo de R9: 1) `lines` null/vacía → `CartEmptyException`; 2) `!storeExists` → `StoreNotFoundException`; 3) `findVariants(skus distintos)` una vez, primer SKU sin vista → `VariantNotFoundException`; 4) `Order.place(OrderId.newId(), …, Instant.now(clock).truncatedTo(MILLIS))` con datos del catálogo; 5) `decreaseStock` por línea **ordenada por SKU**, `false` → `OutOfStockException(sku)`; 6) `orders.save(order)`; 7) devolver `order` (depende de T039–T041, T043, T045; pasa T034)
- [X] T047 [US2] Crear `main/order/application/service/GetOrderService.java` (`@Transactional(readOnly = true)`): `OrderId.parse` vacío, sesión vacía o búsqueda vacía → `OrderNotFoundException` (depende de T045; pasa T035)
- [X] T048 [US2] Crear `res/db/migration/V5__order_schema.sql` con `order_order` y `order_line` **exactamente** como data-model §7 (tipos, `NOT NULL`, `CHECK (sub_total >= 0)`, `CHECK (tax >= 0)`, `CHECK (total >= 0)`, `CHECK (status IN ('PLACED'))`, `placed_at TIMESTAMP WITH TIME ZONE`, `extra_pickups VARCHAR(500)` nullable, `unit_price NUMERIC(10,2) CHECK (unit_price > 0)`, `quantity CHECK (quantity BETWEEN 1 AND 99)`, `position CHECK (position >= 0)`, `PRIMARY KEY (order_id, position)`, FK solo `order_line.order_id → order_order(id)`; sin FKs a catálogo ni inventario)
- [X] T049 [US2] Crear `main/order/infrastructure/persistence/entity/OrderEntity.java` (`@Entity @Table(name = "order_order")`, campos 1:1, `@ElementCollection @CollectionTable(name = "order_line", joinColumns = @JoinColumn(name = "order_id")) @OrderColumn(name = "position") List<OrderLineEmbeddable> lines`), `main/order/infrastructure/persistence/entity/OrderLineEmbeddable.java` (`@Embeddable`; `unitPrice` con `@Column(precision = 10, scale = 2)`), `main/order/infrastructure/persistence/repository/JpaOrderRepository.java` (`JpaRepository<OrderEntity, UUID>` + `@EntityGraph(attributePaths = "lines") Optional<OrderEntity> findByIdAndSessionId(UUID id, UUID sessionId)`) y `main/order/infrastructure/persistence/adapter/OrderRepositoryAdapter.java` (mapea en ambos sentidos con `Order.restore`) (depende de T041, T048; pasa T036 y `JpaSchemaValidationTests`)
- [X] T050 [P] [US2] Crear `main/order/infrastructure/config/OrderClockConfiguration.java`: `@Bean Clock orderClock()` → `Clock.systemUTC()`
- [X] T051 [US2] Crear en `main/order/infrastructure/web/`: `controller/OrderPaths.java`; `dto/PlaceOrderRequest.java` (`@JsonIgnoreProperties(ignoreUnknown = true)`; `firstName` `@NotBlank @Size(max=100)`, `lastName` `@NotBlank @Size(max=100)`, `storeId` `@NotBlank @Size(max=20)`, `extraPickups` `@Size(max=500)` String opcional, `items` `@Size(max=50) List<@NotNull @Valid OrderItemRequest>` **sin** `@NotEmpty`); `dto/OrderItemRequest.java` (`@JsonIgnoreProperties(ignoreUnknown = true)`; `variantId` `@NotBlank @Size(max=20)`, `quantity` `@NotNull @Min(1) @Max(99) Integer`; **no** declarar `price`, `productId`, `productName`, `variantName` ni `image`); `dto/OrderReceiptResponse.java` y `dto/OrderReceiptItemResponse.java` (forma exacta de `contracts/orders-api.md`); `mapper/OrderDtoMapper.java` (tabla de data-model §8: `variantName` = `sku`, `extraPickups` = `extraPickupList()`, `placedAt` ISO-8601 UTC, importes `Money.amount()`, request → `PlaceOrderCommand`) (depende de T039, T045)
- [X] T052 [US2] Crear `main/order/infrastructure/web/controller/OrderController.java`: `POST /api/v1/orders` → `sessionCookies.resolveOrIssue(...)` **antes** del caso de uso, `@Valid` en el cuerpo, 201 con `Location: /api/v1/orders/{id}`; `GET /api/v1/orders/{id}` → solo `sessionCookies.read(...)` (no crea cookie), sin `@Size` en el id; solo llama a casos de uso; tag `Orders` con `@Operation`/`@ApiResponse` (POST 201 con `Location` y `Set-Cookie`, 400, 404, 409, 500; GET 200, 404, 500) y campos ignorados documentados como "ignored; taken from the catalog" (depende de T008, T046, T047, T051; pasa T037)
- [X] T053 [US2] Crear `test/order/OrderApiIT.java` (`RANDOM_PORT`, `JdbcTemplate` fija stock en `@BeforeEach`): cuerpo exacto del front → 201 + `Location` + cookie + stock descontado (AS-2.1); `regresión: precio enviado por el cliente` (`AU-07-YE` con `price: 4000` → línea 5000, `subTotal` 5000.00, `tax` 1050.00, `total` 6050.00) (AS-2.2); `regresión: id de pedido consultable` y `regresión: líneas con SKU` (`GET` con la misma cookie → recibo **idéntico** al del POST, `variantName` = SKU) (AS-2.3, FR-020); otra cookie, sin cookie, id inexistente e id no UUID → 404 `ORDER_NOT_FOUND` (AS-2.4); `extraPickups` texto y ausente (AS-2.5); `CART_EMPTY`, `VALIDATION_ERROR`, `VARIANT_NOT_FOUND`, `STORE_NOT_FOUND` sin pedido ni descuentos (AS-2.7..2.10); tres líneas con la última sin stock → 409 `OUT_OF_STOCK`, las tres filas de stock intactas y `count(order_order)` sin cambios (AS-2.11, SC-004); dos líneas del mismo SKU que juntas superan el stock → 409 y rollback; varios errores a la vez (una línea con `variantId = ""` y otra de `AU-08-WH` con stock 0) → 400 `VALIDATION_ERROR` (gana la validación) y stock intacto; cookie inválida en POST → cookie nueva y pedido ligado a ella
- [X] T054 [P] [US2] Crear `test/order/OrderConcurrencyIT.java` con `@RepeatedTest(5)`: `AU-08-WH` a 1, dos `POST /orders` en paralelo (`ExecutorService` + `CountDownLatch`, cada uno con su cookie) → status exactamente `{201, 409}`, stock final 0 y un único pedido nuevo (AS-2.12, SC-003)

**Checkpoint (⏱ corte P2, objetivo de la demo)**: `./mvnw verify` en verde; quickstart §4 con curl y §5 con el front real sin cambios (ficha → carrito → checkout → gracias, sin errores de red ni consola).

---

## Phase 5: Guardas de arquitectura (transversal, FR-028..FR-030 y FR-031)

> **Ya no está diferida**: se difirió en la review 004 (2026-11-25) y se retomó el 2026-11-26 en la rama `004-eventos-pedidos` (apilada sobre `004-inventario-pedidos`). Implementada.

**Purpose**: deuda de las reviews 001 #15 y 002 #3/#12. Cada regla nueva lleva su fixture negativa (SC-008). No depende de US3 y no debe recortarse con ella.

**Depende de**: US1 y US2 terminadas (las reglas se validan contra su código real).

- [X] T055 [P] Crear fixtures negativas FR-028: `fixtures/manytomany/shop/infrastructure/persistence/` (entidad `shop_` con `@ManyToMany` dueño sin `@JoinTable`) y `fixtures/secondarytable/shop/infrastructure/persistence/` (entidad con `@SecondaryTable` sin prefijo `shop_`), siguiendo el estilo de `fixtures/collectionprefix/`
- [X] T056 [P] Crear fixtures negativas FR-029: `fixtures/domainjakarta/shop/domain/` (clase de dominio con `@jakarta.validation.constraints.NotNull`) y `fixtures/domaingateway/shop/domain/port/ShopGateway.java` (interfaz `*Gateway` fuera de `domain.port.out`)
- [X] T057 [P] Crear fixtures negativas FR-030: `fixtures/domainapp/shop/` (clase de `domain` que importa una de `application`), `fixtures/appspring/shop/application/` (servicio que usa `ApplicationEventPublisher`) y `fixtures/txoutsideapp/shop/infrastructure/web/` (controller con `@Transactional` directo)
- [X] T058 Añadir en `test/ArchitectureRulesSelfTests.java` un caso negativo por fixture (T055–T057) que espere que la regla correspondiente **falle**, y cambiar el caso existente `violation("portout", ArchitectureRules::domainRepositoriesLiveInPortOut)` (línea 30) para que use `ArchitectureRules::domainPortsLiveInPortOut`; la fixture `portout` debe seguir fallando con la regla nueva; ejecutar y ver rojo (depende de T055–T057)
- [X] T059 Implementar en `test/ArchitectureRules.java` (cada regla construida desde una raíz, como las existentes) según R16: ampliar `TABLES_ARE_PREFIXED_WITH_MODULE` (`@ElementCollection` sin `@CollectionTable` explícito, `@ManyToMany` dueño sin `@JoinTable`, y `@SecondaryTable`/`@SecondaryTables` con prefijo); ampliar `DOMAIN_IS_FRAMEWORK_FREE` con `jakarta..` e `io.swagger..`; sustituir `DOMAIN_REPOSITORIES_LIVE_IN_PORT_OUT` y su método `domainRepositoriesLiveInPortOut(String)` (líneas 42 y 149) por `DOMAIN_PORTS_LIVE_IN_PORT_OUT` / `domainPortsLiveInPortOut(String)`, sin dejar el nombre antiguo (toda interfaz de `..domain..` y toda clase `*Repository`/`*Gateway` de `..domain..` en `..domain.port.out..`); nuevas `DOMAIN_DOES_NOT_DEPEND_ON_APPLICATION`, `APPLICATION_USES_ONLY_SERVICE_AND_TRANSACTIONAL` (solo `stereotype.Service` y `transaction.annotation.{Transactional, Propagation, Isolation}`) y `TRANSACTIONAL_ONLY_IN_APPLICATION` (anotación **directa**, de Spring o `jakarta.transaction`; un `@ApplicationModuleListener` meta-anotado no viola) (depende de T058; pasa T058)
- [X] T060 Registrar las reglas nuevas en `test/HexagonalArchitectureTests.java`, sustituir el campo `domainRepositoriesLiveInPortOut = ArchitectureRules.DOMAIN_REPOSITORIES_LIVE_IN_PORT_OUT` (línea 42) por la regla renombrada, comprobar con `grep` que no queda ninguna referencia a `DOMAIN_REPOSITORIES_LIVE_IN_PORT_OUT` ni a `domainRepositoriesLiveInPortOut` en `src/test`, y comprobar que el código de producción (catálogo de 002 + inventory/cart/order de 004) las cumple; mantener `allowEmptyShould` en `API_DOES_NOT_EXPOSE_INTERNALS` (cart.api y notifications.api siguen vacíos) (depende de T059)
- [X] T061 [P] (los 9 paths ya los comprueba `ApplicationIT` desde el commit `20974b6`; quedan los tags) Ampliar `test/ApplicationIT.java` para que el documento OpenAPI tenga **9** paths (5 de Catalog + `/api/v1/inventory/{sku}`, `/api/v1/cart`, `/api/v1/orders`, `/api/v1/orders/{id}`) con los tags `Inventory`, `Cart` y `Orders` (FR-031)
- [X] T061a Ampliar `TABLES_ARE_PREFIXED_WITH_MODULE`: un `@OneToMany` unidireccional (sin `mappedBy`) sin `@JoinColumn`/`@JoinColumns` debe declarar `@JoinTable` con el prefijo del módulo; fixture negativa `fixtures/onetomany/shop/infrastructure/persistence/` y caso en `ArchitectureRulesSelfTests` (regla pendiente de FR-028; `ProductEntity.variants` la cumple con `@JoinColumn`)

**Checkpoint**: `./mvnw test` en verde con `ArchitectureRulesSelfTests` (un rojo esperado por fixture) y `ModularityTests`.

---

## Phase 6: User Story 3 - Aviso de pedido confirmado por evento con outbox (Priority: P3) ✂️ recortable

> **Ya no está diferida**: se difirió en la review 004 (2026-11-25) y se retomó el 2026-11-26 en la rama `004-eventos-pedidos` (apilada sobre `004-inventario-pedidos`). Implementada.

**Goal**: al confirmar un pedido se publica `OrderPlaced` en la misma transacción, se guarda en `event_publication` y `notifications` lo consume y escribe el log; un fallo del listener deja el evento pendiente para reintento.

**Independent Test**: confirmar un pedido y ver en el log `Confirmation email sent for order <id>` y la fila `COMPLETED` en `event_publication` (quickstart §6); `OrderPlacedRetryIT` cubre el reintento.

**Depende de**: US2 (`PlaceOrderService`). Se puede omitir sin tocar P1/P2.

### Tests for User Story 3 (rojo primero) ⚠️

- [X] T062 [US3] Ampliar `test/order/application/service/PlaceOrderServiceTest.java` con `OrderEventsGateway` mockeado: en éxito se llama **una** vez a `orderPlaced(order)` después de `save`; en cada fallo (`CART_EMPTY`, `STORE_NOT_FOUND`, `VARIANT_NOT_FOUND`, `OUT_OF_STOCK`) ni cuando `orders.save` lanza una excepción, no se llama (AS-3.1, AS-3.2)
- [X] T063 [P] [US3] Crear `test/order/OrderPlacedEventIT.java` con `@ApplicationModuleTest(mode = BootstrapMode.ALL_DEPENDENCIES)` + `Scenario` como parámetro del test, y Testcontainers (`TestcontainersConfiguration`), porque `order` necesita los beans de `catalog` e `inventory`. **Antes de escribirlo**, verificar contra el jar `spring-modulith-test-2.1.1` que existe `ApplicationModuleTest.BootstrapMode.ALL_DEPENDENCIES` (`javap -cp <jar> 'org.springframework.modulith.test.ApplicationModuleTest$BootstrapMode'`; confirmado en el analyze del 2026-11-25) y que el contexto arranca con `shared` (módulo abierto). Casos: un pedido válido publica un único `OrderPlaced` con `orderId`, `sessionId` y `lines[sku, quantity]`; un pedido sin stock no publica ninguno
- [X] T064 [P] [US3] Crear `test/notifications/OrderPlacedRetryIT.java`: registrar con una `@TestConfiguration` un bean `@Primary` de `NotifyOrderPlacedUseCase` que es un **doble que falla la primera llamada** (lanza `IllegalStateException`) y en las siguientes **delega en el `NotifyOrderPlacedService` real** (no usar `@MockitoBean`, que impediría que se escriba el log). Tras confirmar el pedido, la publicación queda incompleta (esperar con Awaitility); `IncompleteEventPublications.resubmitIncompletePublications(p -> true)` la entrega y `CompletedEventPublications.findAll()` la contiene; la salida capturada (`OutputCaptureExtension`) contiene `Confirmation email sent for order <orderId>`; el pedido **no** se deshace (AS-3.3, FR-024, SC-006)
- [X] T065 [P] [US3] Crear `test/notifications/application/service/NotifyOrderPlacedServiceTest.java` (sin Spring, `@ExtendWith(OutputCaptureExtension.class)`): `notify(orderId)` escribe una línea INFO `Confirmation email sent for order <orderId>` (AS-3.1)

### Implementation for User Story 3

- [X] T066 [P] [US3] Crear `main/order/api/OrderPlaced.java`: `record OrderPlaced(UUID orderId, UUID sessionId, List<Line> lines)` con `record Line(String sku, int quantity)` (solo tipos del JDK; nombre en pasado)
- [X] T067 [US3] Crear el puerto `main/order/domain/port/out/OrderEventsGateway.java` (`void orderPlaced(Order order)`) e invocarlo en `main/order/application/service/PlaceOrderService.java` como paso 7, tras `orders.save(order)` y dentro de la misma transacción (pasa T062)
- [X] T068 [US3] Añadir `org.springframework.modulith:spring-modulith-starter-jpa` (sin versión; la fija el BOM 2.1.1 ya importado) a `pom.xml`
- [X] T069 [US3] Crear `res/db/migration/V6__event_publication_registry.sql` copiando **literalmente** el DDL v2 PostgreSQL de Modulith 2.1.1 de research R11 (`event_publication` con `id UUID`, `listener_id TEXT`, `event_type TEXT`, `serialized_event TEXT`, `publication_date TIMESTAMP WITH TIME ZONE NOT NULL`, `completion_date`, `status TEXT`, `completion_attempts INT`, `last_resubmission_date`, PK `id` y los dos índices `event_publication_serialized_event_hash_idx` (hash) y `event_publication_by_completion_date_idx`); sin tabla de archivo
- [X] T070 [US3] Configurar en `res/application.yml`: `spring.modulith.events.jdbc.schema-initialization.enabled: false` (explícito) y `spring.modulith.events.republish-outstanding-events-on-restart: true` con comentario de que vale para una sola réplica (R13); sin `completion-mode: archive`
- [X] T071 [US3] Crear `main/order/infrastructure/event/SpringOrderEventsGateway.java` (`@Component implements OrderEventsGateway`): traduce `Order` → `OrderPlaced` y llama a `ApplicationEventPublisher.publishEvent` (depende de T066–T070; pasa T063)
- [X] T072 [US3] Crear `main/notifications/application/port/in/NotifyOrderPlacedUseCase.java` (`void notify(UUID orderId)`), `main/notifications/application/service/NotifyOrderPlacedService.java` (`@Service`; `log.info("Confirmation email sent for order {}", orderId)`) y `main/notifications/infrastructure/event/OrderPlacedListener.java` (`@ApplicationModuleListener void on(OrderPlaced event)` → `notify(event.orderId())`; solo usa `order.api`) (depende de T066; pasa T065 y T064)
- [X] T073 [US3] Ejecutar `./mvnw test -Dtest=ModularityTests` (verde) y la prueba manual de quickstart §7: añadir temporalmente un listener de `OrderPlaced` en `inventory`, comprobar que el build **falla** por el ciclo y revertirlo (FR-025)

**Checkpoint**: `./mvnw verify` en verde con `OrderPlacedEventIT` y `OrderPlacedRetryIT`; quickstart §6 (log + fila `COMPLETED`).

---

## Phase 7: Polish & Cross-Cutting Concerns

> **Ya no está diferida**: se difirió en la review 004 (2026-11-25) y se retomó el 2026-11-26 en la rama `004-eventos-pedidos` (apilada sobre `004-inventario-pedidos`). Completada.

- [X] T074 [P] Revisar Swagger UI en `http://localhost:5271/swagger-ui.html`: tags `Inventory`, `Cart` y `Orders` con descripción, respuestas y errores de las 4 operaciones (quickstart §8, FR-031). Comprobado el 2026-11-26: los 3 tags con descripción, `POST /orders` (201/400/404/409/500), `GET /orders/{id}` (200/404/500), `GET /inventory/{sku}` (200/400/404/500), `GET /cart` (200/500) y las 5 de `Catalog`
- [X] T075 [P] Anotar en `docs/backlog.md` (o en la spec de despliegue `despliegue-azure-esqueleto`) los riesgos de R4 (`SameSite=Lax` con dominios distintos en Azure) y R13 (`republish-outstanding-events-on-restart` con varias réplicas). Hecho en la fila 006\* `despliegue-azure-esqueleto` del backlog, junto con mismo origen o `SameSite=None; Secure` + CORS, límites de frecuencia y de cuerpo (64 KB) en el ingress y `nosniff` (review del PR #5, A04)
- [X] T076 Ejecutar `./mvnw spotless:apply` y `./mvnw verify` completo; comprobar cobertura JaCoCo de las clases nuevas para el Quality Gate de SonarCloud (ADR 0010)
- [X] T077 Recorrer `specs/004-inventario-pedidos/quickstart.md` de §1 a §8 contra una BD vacía y marcar cualquier desviación. Hecho el 2026-11-26 con un Postgres desechable (`docker compose -p ts-qs004`, `DB_PORT=55433`) para no borrar la BD local: §1–§8 sin desviaciones de comportamiento, incluido §5 con el front real (sin errores de consola ni peticiones fallidas). Solo se corrigió texto desactualizado del quickstart (rama y `docker compose down -v`)
- [X] T078 Comprobar SC-007 en local: `POST /orders` y `GET /orders/{id}` < 1 s cada uno (`curl -w '%{time_total}'`). Medido el 2026-11-26, 5 repeticiones: `POST` 16–22 ms, `GET` 6–7 ms

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende de Setup; **bloquea** todas las historias.
- **US1 (Phase 3)**: depende de Phase 2.
- **US2 (Phase 4)**: depende de Phase 2 y de la `InventoryApi` de US1 (T019, T020, T022). Las partes de catálogo y dominio de pedido (T028–T033, T038–T044) pueden empezar en paralelo con US1.
- **Guardas (Phase 5)**: depende de US1 y US2 (valida su código).
- **US3 (Phase 6)**: depende de US2 (`PlaceOrderService`). Recortable.
- **Polish (Phase 7)**: al final de lo que se haya entregado.

### User Story Dependencies

- **US1 (P1)**: independiente tras Phase 2.
- **US2 (P2)**: usa `InventoryApi` de US1 y `SessionCookies` de Phase 2; su test independiente sigue siendo válido (quickstart §4).
- **US3 (P3)**: extiende `PlaceOrderService` de US2.

### Within Each User Story

- Tests escritos y en **rojo** antes de implementar.
- Dominio → puertos → casos de uso → migración/persistencia → web → IT E2E.
- Commit al cerrar cada grupo lógico (p. ej. dominio de inventario, persistencia, web).

### Parallel Opportunities

- Phase 2: T003, T004 en paralelo; T005 y T007 en paralelo.
- US1: todos los tests T010–T016 en paralelo; T017–T019 en paralelo; la rama `cart` (T024–T025) en paralelo con la de `inventory` (T020–T023); T026 y T027 en paralelo.
- US2: tests T028–T037 en paralelo; T038, T040, T041, T042, T050 en paralelo; la rama de catálogo (T042–T044) en paralelo con el dominio de pedido (T038–T041).
- Guardas: fixtures T055–T057 y T061 en paralelo.
- US3: T063, T064, T065 y T066 en paralelo.

---

## Parallel Example: User Story 1

```bash
# Tests en rojo, todos a la vez:
Task: "StockItemTest en src/test/java/com/tractorstore/inventory/domain/model/StockItemTest.java"
Task: "GetStockServiceTest en src/test/java/com/tractorstore/inventory/application/service/GetStockServiceTest.java"
Task: "StockPersistenceIT en src/test/java/com/tractorstore/inventory/infrastructure/persistence/adapter/StockPersistenceIT.java"
Task: "CartControllerTest en src/test/java/com/tractorstore/cart/infrastructure/web/controller/CartControllerTest.java"

# Implementación en dos ramas independientes:
Task: "inventory: StockItem, StockRepository, InventoryApi (T017–T019)"
Task: "cart: Cart, GetCartUseCase, GetCartService (T024)"
```

## Parallel Example: User Story 2

```bash
Task: "MoneyTest / VatTest / OrderIdTest / OrderTest en src/test/java/com/tractorstore/order/domain/model/"
Task: "CatalogApiAdapterTest + CatalogPersistenceIT ampliado (rama catálogo)"
Task: "Money, Vat, OrderId, OrderStatus en src/main/java/com/tractorstore/order/domain/model/ (T038)"
Task: "CatalogApi + VariantView en src/main/java/com/tractorstore/catalog/api/ (T042)"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1 + Phase 2.
2. Phase 3 (US1) → **parar y validar**: quickstart §3 y la ficha del front con "N in stock"/"Out of stock".

### Incremental Delivery (prioridad de la demo del 2026-11-25)

1. Setup + Foundational → base lista.
2. US1 → ⏱ corte P1 (stock y cookie con el front).
3. US2 → ⏱ corte P2: **objetivo mínimo para la demo** (recorrido completo ficha → gracias).
4. Guardas de arquitectura → deuda saldada, build protegido.
5. US3 → outbox y notificaciones; si aprieta el plazo, se recorta sin tocar lo anterior.
6. Polish.

### Notes

- [P] = ficheros distintos, sin dependencias pendientes.
- Verificar que cada test falla antes de implementarlo.
- Cerrar con `/review-spec`, `/pr` y `/log-practica` (flujo del `CLAUDE.md`).
- `/speckit-analyze` (2026-11-25): aplicados C1, I1, I2, U1, G1, G2, G3, A1 y F1. G4 (test de que
  el perfil `prod` activa `Secure` en `ts_session`) se difirió a la fila 010 y se resolvió después
  en esta misma spec con `SessionCookieProfileTests` (review del PR #5, A03); F2 y D1 aceptados sin cambios.
