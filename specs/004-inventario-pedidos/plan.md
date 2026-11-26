# Implementation Plan: Inventario y pedidos (stock, sesión, pedido y evento `OrderPlaced`)

**Branch**: `004-inventario-pedidos` | **Date**: 2026-11-24 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/004-inventario-pedidos/spec.md` + indicaciones del
`/speckit-plan`: P1 y P2 funcionando contra el front esta noche; P3 (outbox + notificaciones) al
final; migraciones desde V4; DDL del Event Publication Registry verificado contra Spring Modulith
2.1.1; estructura hexagonal de la skill; Constitution Check contra la v1.3.0.

## Summary

Tres módulos de dominio reciben código (`inventory`, `cart`, `order`), `notifications` recibe su
primer listener y `catalog` abre por fin su cara pública. Cada uno sigue las capas hexagonales de la
skill (`api` / `domain` / `application` / `infrastructure`).

- **P1**: `GET /inventory/{sku}` lee `inventory_stock` (sembrada en `V4`, `CL-01-GY` a 0) y
  `GET /cart` devuelve el carrito vacío y fija `ts_session` con un componente único de
  `shared.infrastructure.web` (`SessionCookies`).
- **P2**: `POST /orders` abre **una** transacción en `PlaceOrderService`. En ella valida la tienda y
  las variantes por `catalog.api` (precio real, nombres, imagen), calcula subtotal, IVA 21 %
  `HALF_UP` y total en el dominio (`Money`, `Vat`) y descuenta stock por `inventory.api` con un
  `UPDATE … WHERE stock >= :qty`, en orden de SKU para no provocar *deadlocks*. Después guarda el
  pedido en `order_order` + `order_line` (`V5`). `GET /orders/{id}` busca por id **y** sesión, así
  que otra sesión recibe 404.
- **P3**: `OrderPlaced` (record en `order.api`) se publica por un puerto de salida y lo consume solo
  `notifications`, con `@ApplicationModuleListener` y el Event Publication Registry de
  `spring-modulith-starter-jpa`. La tabla se crea en `V6` copiando literalmente el DDL v2 de
  PostgreSQL del jar 2.1.1.

También entran las tres guardas de arquitectura pendientes (FR-028 a FR-030), cada una con su
fixture negativa. Decisiones y alternativas en [research.md](research.md).

## Technical Context

**Language/Version**: Java 21 (Temurin)

**Primary Dependencies**: Spring Boot 4.1.2 (`webmvc`, `validation`, `data-jpa`, `flyway`,
`actuator`), Spring Modulith 2.1.1, springdoc-openapi 3.2.0, Hibernate 7, Jackson 3. **Dependencia
nueva solo en P3**: `org.springframework.modulith:spring-modulith-starter-jpa` (versión del BOM
2.1.1 ya importado; arrastra `events-jpa`, `events-core`, `events-jackson`). P1 y P2 no tocan
`pom.xml`.

**Storage**: PostgreSQL 17. Flyway `V4__inventory_stock.sql`, `V5__order_schema.sql`,
`V6__event_publication_registry.sql` (P3). `ddl-auto=validate` (también valida la entidad JPA del
registro de Modulith), `open-in-view=false`. Dinero `NUMERIC(10,2)` ↔ `BigDecimal` de escala 2.

**Testing**: JUnit 5 + AssertJ + Mockito; `@WebMvcTest` + `@MockitoBean`; `@DataJpaTest` +
Testcontainers (`postgres:17.12`); `@SpringBootTest(RANDOM_PORT)` + `RestTestClient`;
`@ApplicationModuleTest` + `Scenario` (P3); ArchUnit 1.5.1 + `ArchitectureRulesSelfTests`; Modulith
`verify()`.

**Target Platform**: servidor Linux en contenedor; local con Docker Compose (perfil `dev`, puerto 5271)

**Project Type**: web-service (monolito modular)

**Performance Goals**: confirmar pedido y leer recibo < 1 s en local (SC-007). Por petición: una
consulta de variantes al catálogo (no N+1) y un `UPDATE` por línea.

**Constraints**: forma JSON exacta del front (contrato + ADR 0011); front sin cambios; todo o nada
en la confirmación; stock nunca negativo con concurrencia; demo el 2026-11-25 (P1+P2 hoy, P3 lo
primero que se recorta).

**Scale/Scope**: 4 endpoints, 3 migraciones, 4 tablas (`inventory_stock`, `order_order`,
`order_line`, `event_publication`), ~45 clases de producción y ~25 de test.

Sin `NEEDS CLARIFICATION`: las dudas de la spec se cerraron en `/speckit-clarify` y las técnicas
están en [research.md](research.md) (R1–R16).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.* Contrastado con la
constitución **v1.3.0** (2026-11-24).

| Principio | Cómo lo cumple este plan | Estado |
|---|---|---|
| I. Contrato primero | Las 4 rutas, campos y códigos salen de `docs/api-contract.md` y la ADR 0011, que ya están reescritos y enmendados (`STORE_NOT_FOUND`, FR-011a). [contracts/](contracts/) no añade ningún campo ni código. Los controllers devuelven records de `infrastructure.web.dto`, nunca entidades. Hay un `@WebMvcTest` por endpoint (status, JSONPath y error con `traceId`), incluido el cuerpo exacto de `cart.service.ts`. | ✅ |
| II. Monolito modular + hexagonal | La cookie vive en `shared.infrastructure.web.SessionCookies`, que la v1.3.0 permite en `shared`; se actualiza el comentario de `shared/package-info.java`, pendiente del Sync Impact Report. Cada módulo tiene `api`/`domain`/`application`/`infrastructure`. Los controllers solo llaman a `*UseCase`. `order` usa solo `catalog.api` e `inventory.api`, y `notifications` solo `order.api`. Los `allowedDependencies` ya están declarados y coinciden con FR-026. Se añade el subpaquete `infrastructure/event` en `order` y `notifications` (ver Complexity Tracking). | ✅ (1 desviación justificada) |
| III. Pruebas primero | Cada paso del orden de implementación empieza en rojo y va de dominio a aplicación y de ahí a adaptadores. Testcontainers con PostgreSQL real; el enforcer prohíbe H2. Regresiones .NET con `@DisplayName("regresión: …")`: precio enviado por el cliente, id de pedido consultable y líneas con SKU. | ✅ |
| IV. Datos | Tablas `inventory_stock`, `order_order` y `order_line`, sin FKs ni JOINs entre módulos (la FK `order_line → order_order` es interna del módulo). Flyway `V4`–`V6`; `V1`–`V3` no se tocan. `NUMERIC(10,2)`/`BigDecimal`. El precio viene **siempre** de `catalog.api` y el `price` del cuerpo ni siquiera se enlaza en el DTO. `event_publication` no lleva prefijo porque es la tabla del framework que exige el principio V (R12). | ✅ |
| V. Eventos internos | `OrderPlaced` es un record en pasado en `order.api`. El único consumidor es `notifications`, con `@ApplicationModuleListener`, y la entrega va por el Event Publication Registry con tabla Flyway (DDL 2.1.1 verificado, R11) y sin inicialización automática. No hay broker. El stock **no** escucha el evento: se descuenta síncrono por `inventory.api` con `UPDATE` condicional, sin ciclo. Tests: `Scenario` y reintento con un listener que falla una vez. | ✅ |
| VI. Seguridad proporcionada | `ts_session` es un UUID aleatorio con `HttpOnly`, `SameSite=Lax` y `Path=/`; `Secure` lo activa la propiedad `tractorstore.session.secure-cookie=true` en `prod`. Un valor que no es UUID se sustituye. Pedido de otra sesión o sin cookie → 404. Bean Validation con límites en cuerpo y ruta (R6). Un id de pedido que no es UUID → 404 sin llegar a la BD. Sin secretos. CORS sin cambios (ya admite `POST`, credenciales y `Location`). Riesgo de despliegue anotado en R4. | ✅ |
| VII. Simplicidad y plazo | Solo una dependencia nueva (P3). Sin carrito en servidor, sin reservas, sin caché, sin reintento programado (basta republicar al arrancar). P3 va al final y se puede recortar sin tocar P1/P2. | ✅ |
| VIII. Entregable y operable | Nuevas propiedades por perfil (`secure-cookie` en `prod`) y health intacto. `republish-outstanding-events-on-restart` documentado para una sola réplica (R13). | ✅ |
| Restricciones técnicas | Boot 4.1.2 con la skill `spring-boot-4-notes` aplicada: Jackson 3 (`tools.jackson`, anotaciones en `com.fasterxml.jackson.annotation`), `@MockitoBean` y Modulith 2.x con esquema v2. Spotless y JaCoCo/Sonar sin cambios de configuración. | ✅ |
| Flujo | Rama `004-inventario-pedidos`, un PR, conventional commits. No hace falta ADR nueva: la ADR 0011 ya cubre contrato, sesión en `shared`, stock síncrono y consumidores. | ✅ |

**Resultado**: pasa. Única desviación: el subpaquete `infrastructure/event`, justificado abajo.

**Re-check post-diseño (Fase 1)**:
- [data-model.md](data-model.md) añade 3 tablas con prefijo y la del registro (excepción R12).
- [contracts/](contracts/) solo usa campos de los modelos TS del front y códigos de la tabla de
  errores.
- La regla FR-030 ("de Spring solo `@Service`/`@Transactional` en `application`") obliga a publicar
  el evento por un puerto de salida (`OrderEventsGateway`) y a poner el listener en
  `infrastructure`. Así se hace.
- Ningún componente fuera de alcance. Sigue en ✅.

## Project Structure

### Documentation (this feature)

```text
specs/004-inventario-pedidos/
├── plan.md              # este archivo
├── research.md          # Fase 0: R1–R16
├── data-model.md        # Fase 1: dominio, puertos, tablas, migraciones, mapeos
├── quickstart.md        # Fase 1: guía de validación ejecutable (curl + front real)
├── contracts/
│   ├── inventory-cart-api.md   # GET /inventory/{sku}, GET /cart, cookie ts_session
│   ├── orders-api.md           # POST /orders, GET /orders/{id}, precedencia de errores
│   └── module-apis.md          # CatalogApi, InventoryApi, OrderPlaced (contratos entre módulos)
├── checklists/          # de /speckit-specify
└── tasks.md             # Fase 2 (/speckit-tasks; no lo crea este comando)
```

### Source Code (repository root)

```text
pom.xml                                            # P3: + spring-modulith-starter-jpa

src/main/java/com/tractorstore/
├── shared/
│   ├── package-info.java                          # comentario: + sesión por cookie (ADR 0011)
│   ├── error/
│   │   ├── ErrorKind.java                         # CONFLICT ya existe; javadoc actualizado
│   │   └── ConflictException.java                 # NUEVA: base de 409 (kind = CONFLICT)
│   └── infrastructure/web/
│       ├── GlobalExceptionHandler.java            # CONFLICT → 409
│       ├── SessionCookies.java                    # NUEVA: read / resolveOrIssue ts_session
│       └── SessionCookieProperties.java           # NUEVA: tractorstore.session.secure-cookie
├── catalog/
│   ├── api/
│   │   ├── CatalogApi.java                        # findVariants(Collection<String>), storeExists(String)
│   │   └── VariantView.java                       # sku, productId, productName, variantName, price, image
│   ├── domain/port/out/
│   │   ├── ProductRepository.java                 # + findByVariantSkus(Collection<String>)
│   │   └── StoreRepository.java                   # + existsById(String)
│   ├── application/
│   │   ├── port/in/FindVariantsUseCase.java, StoreExistsUseCase.java
│   │   └── service/FindVariantsService.java, StoreExistsService.java, CatalogApiAdapter.java
│   └── infrastructure/persistence/
│       ├── repository/JpaProductRepository.java   # + findAllContainingSkus (@Query con subconsulta + EntityGraph)
│       └── adapter/ProductRepositoryAdapter.java, StoreRepositoryAdapter.java
├── inventory/
│   ├── api/InventoryApi.java                      # boolean decreaseStock(String sku, int quantity)
│   ├── domain/
│   │   ├── model/StockItem.java
│   │   ├── exception/StockNotFoundException.java  # VARIANT_NOT_FOUND
│   │   └── port/out/StockRepository.java          # findBySku, decreaseIfAvailable
│   ├── application/
│   │   ├── port/in/GetStockUseCase.java, DecreaseStockUseCase.java
│   │   └── service/GetStockService.java, DecreaseStockService.java, InventoryApiAdapter.java
│   └── infrastructure/
│       ├── web/controller/InventoryController.java, InventoryPaths.java
│       ├── web/dto/StockResponse.java
│       └── persistence/{entity/StockEntity, repository/JpaStockRepository, adapter/StockRepositoryAdapter}
├── cart/
│   ├── domain/model/Cart.java                     # siempre vacío
│   ├── application/port/in/GetCartUseCase.java, service/GetCartService.java
│   └── infrastructure/web/
│       ├── controller/CartController.java, CartPaths.java   # usa SessionCookies
│       └── dto/CartResponse.java, CartItemResponse.java
├── order/
│   ├── api/OrderPlaced.java                       # record + Line(sku, quantity)   [P3]
│   ├── domain/
│   │   ├── model/Order.java, OrderLine.java, OrderId.java, OrderStatus.java, Money.java, Vat.java
│   │   ├── exception/CartEmptyException, StoreNotFoundException, VariantNotFoundException,
│   │   │             OutOfStockException, OrderNotFoundException
│   │   └── port/out/OrderRepository.java, OrderEventsGateway.java [P3]
│   ├── application/
│   │   ├── model/PlaceOrderCommand.java
│   │   ├── port/in/PlaceOrderUseCase.java, GetOrderUseCase.java
│   │   └── service/PlaceOrderService.java, GetOrderService.java
│   └── infrastructure/
│       ├── web/controller/OrderController.java, OrderPaths.java
│       ├── web/dto/PlaceOrderRequest.java, OrderItemRequest.java, OrderReceiptResponse.java,
│       │           OrderReceiptItemResponse.java
│       ├── web/mapper/OrderDtoMapper.java
│       ├── persistence/{entity/OrderEntity, OrderLineEmbeddable; repository/JpaOrderRepository;
│       │               adapter/OrderRepositoryAdapter}
│       ├── event/SpringOrderEventsGateway.java     # [P3] ApplicationEventPublisher
│       └── config/OrderClockConfiguration.java    # Clock UTC
└── notifications/                                 # [P3]
    ├── application/port/in/NotifyOrderPlacedUseCase.java, service/NotifyOrderPlacedService.java
    └── infrastructure/event/OrderPlacedListener.java   # @ApplicationModuleListener

src/main/resources/
├── application.yml          # + tractorstore.session.secure-cookie: false; [P3] spring.modulith.events.*
├── application-prod.yml     # + tractorstore.session.secure-cookie: true
└── db/migration/
    ├── V4__inventory_stock.sql
    ├── V5__order_schema.sql
    └── V6__event_publication_registry.sql          # [P3] DDL v2 literal de Modulith 2.1.1

src/test/java/com/tractorstore/
├── ArchitectureRules.java, HexagonalArchitectureTests.java, ArchitectureRulesSelfTests.java  # FR-028..030
├── archfixtures/{manytomany, secondarytable, domainjakarta, domaingateway, domainapp,
│                 appspring, txoutsideapp}/…       # una violación por regla nueva
├── ApplicationIT.java                             # 9 paths en OpenAPI
├── shared/infrastructure/web/SessionCookiesTest.java, GlobalExceptionHandlerTest.java (+409)
├── catalog/…/CatalogApiAdapterTest.java, CatalogPersistenceIT.java (+ findByVariantSkus, existsById)
├── inventory/
│   ├── domain/model/StockItemTest.java
│   ├── application/service/*ServiceTest.java
│   ├── infrastructure/web/controller/InventoryControllerTest.java
│   ├── infrastructure/persistence/adapter/StockPersistenceIT.java   # 47 filas, ≥1 a 0, UPDATE condicional
│   └── InventoryApiIT.java
├── cart/…/CartControllerTest.java, CartApiIT.java (cookie)
├── order/
│   ├── domain/model/MoneyTest.java, VatTest.java (@ParameterizedTest), OrderTest.java, OrderIdTest.java
│   ├── application/service/PlaceOrderServiceTest.java, GetOrderServiceTest.java
│   ├── infrastructure/web/controller/OrderControllerTest.java      # cuerpo exacto del front
│   ├── infrastructure/persistence/adapter/OrderPersistenceIT.java
│   ├── OrderApiIT.java                            # E2E: cookie, 404 otra sesión, rollback, regresiones
│   ├── OrderConcurrencyIT.java                    # último ejemplar: 201 + 409, stock 0
│   └── OrderPlacedEventIT.java                    # [P3] Scenario
└── notifications/OrderPlacedRetryIT.java          # [P3] listener falla una vez → resubmit → completado
```

**Structure Decision**: mismo proyecto Maven. Cada módulo sigue la plantilla de la skill
`hexagonal-architecture`, y los tests van en el paquete espejo de la clase que prueban. `catalog.api`
recibe sus primeras clases, así que la regla `API_DOES_NOT_EXPOSE_INTERNALS` deja de ejecutarse en
vacío (el `allowEmptyShould` se mantiene porque `cart.api` y `notifications.api` siguen vacíos).

### Orden de implementación (inside-out, test primero, priorizado para la demo)

> 🔴 = test que falla primero; 🟢 = código que lo pasa. Las marcas ⏱ son puntos de corte: lo que
> queda por encima funciona contra el front sin lo de debajo.

**Fase 0 — Base común (bloquea P1 y P2)**
1. 🔴 `GlobalExceptionHandlerTest` (409 con `code` del `ConflictException`) → 🟢 `ConflictException`
   y la rama `CONFLICT` del handler.
2. 🔴 `SessionCookiesTest` (sin cookie, UUID válido, valor no UUID, `Secure` según propiedad,
   atributos) → 🟢 `SessionCookies` + `SessionCookieProperties` + YAML + comentario de
   `shared/package-info.java`.

**Fase P1 — Stock y sesión**
3. 🔴 `StockItemTest` → 🟢 `inventory.domain`.
4. 🔴 `GetStockServiceTest`, `DecreaseStockServiceTest` (puerto mockeado) → 🟢 casos de uso +
   `InventoryApi` + `InventoryApiAdapter`.
5. 🔴 `StockPersistenceIT` (47 filas, `CL-01-GY` = 0, el `UPDATE` condicional devuelve 0 si no
   llega) → 🟢 `V4`, entidad, `JpaStockRepository` (`@Modifying`), adaptador.
6. 🔴 `InventoryControllerTest` → 🟢 controller, DTO, `@Operation`.
7. 🔴 `CartControllerTest` → 🟢 `cart.domain.Cart`, `GetCartUseCase`, controller (usa `SessionCookies`).
8. 🔴 `InventoryApiIT` + `CartApiIT` (cookie real en la respuesta) → 🟢 E2E P1.
   ⏱ **Corte P1**: la ficha muestra "N in stock"/"Out of stock" y el navegador recibe `ts_session`.

**Fase P2 — Confirmar y consultar pedido**
9. 🔴 `MoneyTest`, `VatTest` (0,05 → 0,01; casos `HALF_UP`), `OrderIdTest` (UUID canónico),
   `OrderTest` (totales, `extraPickups` → lista, sesión, invariantes) → 🟢 `order.domain`.
10. 🔴 `CatalogApiAdapterTest` + ampliación de `CatalogPersistenceIT` → 🟢 `CatalogApi`,
    `VariantView`, casos de uso y consultas nuevas del catálogo.
11. 🔴 `PlaceOrderServiceTest` (precedencia: vacío → tienda → variante → stock; precio manipulado;
    orden de descuento por SKU; no guarda si falla) y `GetOrderServiceTest` (otra sesión, sin
    sesión, id no UUID → 404) → 🟢 casos de uso.
12. 🔴 `OrderPersistenceIT` (guardar y releer idéntico: escala, `placedAt` en milisegundos, orden de
    líneas) → 🟢 `V5`, entidades, `JpaOrderRepository`, adaptador, `Clock`.
13. 🔴 `OrderControllerTest` (cuerpo exacto del front, 201 + `Location` + cookie, 400/409/404,
    `extraPickups` array → 400 `MALFORMED_REQUEST`, `[]` cuando falta) → 🟢 controller, DTOs, mapper.
14. 🔴 `OrderApiIT` (regresiones .NET, rollback de varias líneas, otra cookie → 404) +
    `OrderConcurrencyIT` → 🟢 E2E P2.
    ⏱ **Corte P2 (objetivo de esta noche)**: quickstart §5 con el front real.

**Fase Guardas — deuda de arquitectura (FR-028..FR-030)**
15. 🔴 fixtures + `ArchitectureRulesSelfTests` para cada regla nueva → 🟢 `ArchitectureRules` +
    `HexagonalArchitectureTests`; `ApplicationIT` con 9 paths (FR-031).

**Fase P3 — Evento y outbox (recortable)**
16. 🔴 `PlaceOrderServiceTest`: publica `orderPlaced` solo si tiene éxito → 🟢 puerto
    `OrderEventsGateway` + llamada en el caso de uso.
17. 🔴 `OrderPlacedEventIT` (`Scenario`: un único `OrderPlaced` con id, sesión y líneas; ninguno
    si falla) → 🟢 `pom.xml` + `V6` + `SpringOrderEventsGateway` + YAML `spring.modulith.events`.
18. 🔴 `OrderPlacedRetryIT` (falla una vez → pendiente → `resubmitIncompletePublications` →
    completado; log con el `orderId`) → 🟢 `NotifyOrderPlacedUseCase`, servicio y
    `OrderPlacedListener`.
19. `ModularityTests` en verde y comprobación manual de que un listener de `OrderPlaced` en
    `inventory` rompe el build (FR-025, quickstart §7).

## Complexity Tracking

| Desviación | Por qué | Alternativa más simple descartada porque |
|---|---|---|
| Subpaquete `infrastructure/event` en `order` y `notifications` (la constitución II enumera `web`, `persistence`, `config`) | FR-030 prohíbe en `application` cualquier tipo de Spring salvo `@Service`/`@Transactional`. Por eso `ApplicationEventPublisher` (publicar) y `@ApplicationModuleListener` (consumir) son adaptadores. No son web ni persistencia, y `config` queda excluido de la cobertura de Sonar. | Ponerlos en `application` viola FR-030. Meterlos en `config` los saca de la cobertura y mezcla *wiring* con lógica. Meterlos en `persistence` confunde el outbox con los repositorios del módulo. |
| Tabla `event_publication` sin prefijo de módulo (constitución IV) | La exige el principio V: el nombre lo fija la entidad JPA de Modulith (`@Table(name = "EVENT_PUBLICATION")`) y no es configurable. No pertenece a ningún módulo de dominio, igual que `flyway_schema_history`. | Renombrarla no es posible con `starter-jpa`. La regla ArchUnit de prefijos solo mira entidades de `com.tractorstore`, así que no se ve afectada. |
