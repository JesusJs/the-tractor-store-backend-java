# Revisión 004 — 2026-11-25

Alcance: fases 1–4 de `tasks.md` (US1 stock y sesión, US2 pedidos). Las fases 5–7 están pendientes
a propósito y no se evalúan. `./mvnw -q verify` en verde (exit 0): no hay hallazgo BLOQUEANTE por
build. `ModularityTests`, `HexagonalArchitectureTests` y `ArchitectureRulesSelfTests` en verde.

Hallazgos verificados contra el código; los marcados con ✔ se reprodujeron además con la app en
marcha (perfil dev, `localhost:5271`).

| # | Severidad | Revisor | Archivo:línea | Hallazgo | Propuesta |
|---|---|---|---|---|---|
| 1 | MEDIA | spec | `order/infrastructure/web/dto/OrderItemRequest.java:22` | ✔ Jackson convierte tipos en silencio: `"quantity": 1.5` → 201 con cantidad 1; `"quantity": "2"` → 201 con 2; `"variantId": 12` → se trata como `"12"`. Debería ser 400 `MALFORMED_REQUEST` (FR-017, error #1 de `contracts/orders-api.md`). `malformedBodies` solo prueba `"one"`. | Desactivar `ACCEPT_FLOAT_AS_INT` y la coerción texto↔número/escalar (customizer de `JsonMapper` en `shared`, verificando el nombre en Boot 4). Añadir `1.5`, `"2"` y `variantId` numérico a `malformedBodies` y un caso al IT. |
| 2 | MEDIA | seguridad | `order/application/service/PlaceOrderService.java:54-75`; `OrderItemRequest.java:22` | Un solo `POST` anónimo de 50 líneas puede vaciar casi todo el stock sembrado (~470 uds.) y dejar la demo en 409. Repetir el SKU en varias líneas esquiva `@Max(99)` por SKU. Sin límite de frecuencia. | Para la demo: tener listo un script que restaure el seed y limitar frecuencia en el ingress (spec de despliegue). Opcional: rechazar SKUs repetidos o validar Σ cantidad por SKU ≤ 99 (cambia la Clarification de líneas duplicadas: requiere decisión). |
| 3 | BAJA | arquitectura | `inventory/application/service/DecreaseStockService.java:10` | `@Transactional` con `REQUIRED`: si un llamante futuro no tiene transacción, el descuento se confirma por su cuenta y se pierde la atomicidad sin aviso. `InventoryApi` y `module-apis.md` dicen "necesita una transacción activa". Hoy el único llamante (`PlaceOrderService`) sí la tiene; data-model §2 fija `REQUIRED`. | `propagation = Propagation.MANDATORY` + test que espere `IllegalTransactionStateException` sin transacción; ajustar data-model §2. |
| 4 | BAJA | seguridad | `PlaceOrderRequest.java:17-23`; `OrderItemRequest.java:21` | ✔ Un `\u0000` en `firstName` o `variantId` da 500 `INTERNAL_ERROR` (`PSQLException: invalid byte sequence … 0x00`) con stacktrace en el log, en vez de 400. Sin pérdida de stock. | `@Pattern` en `variantId`/`storeId` (`^[A-Za-z0-9-]+$`) y sin caracteres de control en nombres y `extraPickups`; test que espere 400 `VALIDATION_ERROR`. |
| 5 | BAJA | spec | `order/infrastructure/web/controller/OrderController.java:31-32, 86` | ✔ Un `POST /orders` que acaba en 400 (`@Valid` o JSON ilegible) no emite `Set-Cookie`: el javadoc dice que la cookie "also reaches a failed response", y solo es cierto para 404/409. Sin efecto con el front real (llama antes a `GET /cart`). | Corregir el javadoc y anotarlo en `contracts/orders-api.md` (recomendado), o emitir la cookie en un interceptor. |
| 6 | BAJA | spec | `src/test/java/com/tractorstore/order/OrderApiIT.java:140` | Dos regresiones .NET ("id de pedido consultable" y "líneas con SKU") comparten un único `@DisplayName`; T053 pide uno por regresión. Ambas están probadas. | Partir en dos tests con su `@DisplayName("regresión: …")`. |
| 7 | BAJA | spec | rama (`git log main..HEAD`: 31 commits) | La rama parte de `003-calidad-sonar`, aún sin fusionar en `main`: un PR de 004 contra `main` arrastraría la spec 003. | Fusionar antes el PR de 003 y hacer rebase de 004 sobre `main` antes de `/pr`. |
| 8 | BAJA | seguridad | `shared/infrastructure/web/SessionCookies.java:17, 37-47` | Se acepta cualquier UUID canónico aunque no lo haya emitido el backend (fijación de sesión si alguien puede plantar la cookie, p. ej. desde un subdominio). El comentario de la línea 17 promete más de lo que hace. Explotación poco probable: además hay que conocer el `orderId`. | Corregir el comentario ya. En prod (spec de despliegue), valorar el prefijo `__Host-`. |
| 9 | BAJA | seguridad | `PlaceOrderRequest.java:24` | No hay límite de tamaño del cuerpo: `@Size(max=50)` se comprueba después de deserializar todo (un JSON de varios MB se parsea entero). | Límite en el ingress o en Tomcat para la spec de despliegue (p. ej. 64 KB); anotarlo en el backlog (T075). |
| 10 | BAJA | arquitectura | `order/domain/model/Order.java:125`; `inventory/domain/model/StockItem.java:19` | `Order.belongsTo` y `StockItem.canServe` solo los usan los tests (VII, YAGNI). Están en data-model §2 y §5. | Dejarlos a sabiendas o quitarlos del código y de data-model. |

## Descartados o solo informativos

- **`Optional<UUID>` como parámetro en `GetOrderUseCase`** (arquitectura, regla Sonar S3553): lo
  fija data-model §5 y el impacto en el Quality Gate es un *code smell* menor. Informativo.
- **`FindVariantsUseCase` devuelve `catalog.api.VariantView`** (arquitectura): coherente con
  data-model §4; separar el modelo no compensa hoy (VII). Informativo.
- **"Edge BD caída" sin IT propio** (spec): el revisor lo da por cubierto por el test unitario de
  propagación y el IT de rollback de `OUT_OF_STOCK`.

## Decisiones (2026-11-25)

| # | Decisión | Qué se hizo | Commit |
|---|---|---|---|
| 1 | Corregir | Sin coerción en Jackson 3: `spring.jackson.deserialization.accept-float-as-int: false` y `spring.jackson.mapper.allow-coercion-of-scalars: false` en `application.yml` (nombres verificados en los metadatos de `spring-boot-jackson` 4.1.2 y en `jackson-databind` 3.1.7). Número → texto no lo cubre ninguna propiedad: `JsonCoercionConfiguration` (`shared`) añade un `JsonMapperBuilderCustomizer` con `CoercionAction.Fail` para `LogicalType.Textual`. `"quantity": 1.5`, `"quantity": "2"` y `"variantId": 12` → 400 `MALFORMED_REQUEST` en `OrderControllerTest` y `OrderApiIT` (sin pedido ni descuento). Se comprobó que sin las dos propiedades los casos `1.5` y `"2"` vuelven a fallar. | `5507c89` |
| 2 | Corregir en parte | `scripts/reset-demo-stock.sh` restaura `inventory_stock` con el `INSERT` de `V4__inventory_stock.sql` (fuente única del seed) en la BD dev; `DB_PORT` configurable, por defecto `55432`; solo host local; conserva los pedidos. Documentado en quickstart §4.1. **Diferido a la fila 010 del backlog**: límite de frecuencia (ingress) y límite de Σ cantidad por SKU (cambia la Clarification de líneas duplicadas). | `2870310` |
| 3 | Corregir | `DecreaseStockService` con `@Transactional(propagation = MANDATORY)`; `InventoryApiIT` comprueba que sin transacción lanza `IllegalTransactionStateException` y el stock no cambia. Ajustados `InventoryApi` (javadoc), data-model §2 y `contracts/module-apis.md`. | `1ea691a` |
| 4 | Corregir | `@Pattern`: `variantId` y `storeId` solo `[A-Za-z0-9-]`; `firstName`, `lastName` y `extraPickups` sin caracteres de control (`\P{Cc}`). `\u0000` → 400 `VALIDATION_ERROR` (unitario con cinco casos e IT contra PostgreSQL sin cambios en BD). Tabla de límites de `contracts/orders-api.md` actualizada. | `bb5ccaf` |
| 5 | Corregir (documentar) | Javadoc de `OrderController` y descripción OpenAPI de `Set-Cookie`: un 400 no emite cookie; los 404/409/500 sí. Anotado en `contracts/orders-api.md`. | `d133000` |
| 6 | Corregir | `OrderApiIT`: `regresión: id de pedido consultable` y `regresión: líneas con SKU` son ahora dos tests. | `a45fa72` |
| 7 | Resuelto | 003 fusionada en `main` (PR #3) y 004 rebasada sobre `main`: `git merge-base --is-ancestor main HEAD` se cumple. | — |
| 8 | Corregir el comentario | Javadoc de `SessionCookies`: cualquier UUID canónico se acepta aunque no lo haya emitido el backend. **Diferido a la fila 010**: prefijo `__Host-` en prod. | `ee25fe6` |
| 9 | Diferido | Límite de tamaño del cuerpo (ingress o Tomcat, p. ej. 64 KB) → fila 010 del backlog. | — |
| 10 | Aceptado | `Order.belongsTo` y `StockItem.canServe` se quedan (están en data-model §2 y §5). | — |

**Fases 5, 6 y 7 de `tasks.md`** (guardas ArchUnit FR-028..FR-030, P3 `OrderPlaced` + outbox +
`notifications`, y pulido): se difirieron en esta review; **ya no están diferidas**: se retomaron
el 2026-11-26 en la rama `004-eventos-pedidos`, apilada sobre esta (anotado en `tasks.md` y en
`docs/backlog.md`). La rama `004-inventario-pedidos` cierra P1 y P2.

## Review del PR #5 (Oscar) — decisiones del 2026-11-26

Hilos de `oscarmoraquind` en el PR #5, aplicados en un único push. Ajustan decisiones de arriba: #2
(puerto por defecto del script → A05), #4 (`\P{Cc}` → A07) y #9 (límite de cuerpo → fila 006\*, A04).
Fechas del backlog acordadas el 2026-11-26, sujetas a confirmación con el mentor.

| ID | Severidad | Decisión | Qué se hizo | Commit |
|---|---|---|---|---|
| A01 | MEDIA | Corregir (decisión del mentor) | Carrito sin servidor registrado como **desviación temporal** en `docs/requisitos.md` (tabla de desviaciones y requisito 4) y en el Estado de la ADR 0011, cerrada por la spec 005\* `carrito-servidor` (2026-12-01): 4 endpoints de la guía con `ts_session`, precio real de la variante y stock, y `CartService` del front consumiéndolos. Fila 005\* en el backlog. Issue del front: [JesusJs/the-tractore-store#1](https://github.com/JesusJs/the-tractore-store/issues/1). Mitigación para la demo: tabla de SKUs con precio = "desde" en `quickstart.md` §5.1 (los 7 productos afectados coinciden con los de la review). | `1a00925` |
| A02 | MEDIA | Corregir | Demo y cierre el 2026-12-03 en `CLAUDE.md:4` y `constitution.md` (VII, dentro de la 1.3.0, sin 1.3.1). Backlog: cierre 003 2026-11-27, continuación 004 2026-11-30 (rama `004-eventos-pedidos`), 005\* `carrito-servidor` 2026-12-01, 006\* `despliegue-azure-esqueleto` 2026-12-02 con plan B (demo local con `docker compose`), nota de fechas sujetas a confirmación. `ts_session` en la descripción de `shared` de `CLAUDE.md`. | `caac6be` |
| A03 | MEDIA | Corregir | `SessionCookieProfileTests` (unitario, `YamlPropertySourceLoader`, patrón de `JpaSchemaValidationTests`): `application.yml` → `secure-cookie` `false`; `application-prod.yml` → `true`. G4 del analyze y la fila 010 actualizados. | `3159704` |
| A04 | MEDIA | Corregir | Fila 006\* del backlog: mismo origen (ADR 0004) o `SameSite=None; Secure` + CORS; límite de frecuencia de `POST /orders` y de cuerpo de 64 KB en el ingress; `X-Content-Type-Options: nosniff`; R13. Salen de la fila 010. T075 marcado. | `ef9bc09` |
| A05 | BAJA | Corregir | `scripts/reset-demo-stock.sh` con `DB_PORT` por defecto `5432` (como `compose.yaml`) y comentarios corregidos; sin `psql` en el equipo usa `docker compose exec -T postgres psql`. `quickstart.md` §4.1 y `contexto-sesion.md` corregidos. Probado con las dos ramas contra la BD dev: 47 SKUs restaurados. | `93651f5` |
| A06 | BAJA | Corregir | `shared.infrastructure.web.InputPatterns.CATALOG_ID` compartida por `PlaceOrderRequest`/`OrderItemRequest` y el `{sku}` de `InventoryController` (`@Pattern`) → 400 `VALIDATION_ERROR` sin reflejar la entrada. `@ApiResponse` 400, `contracts/inventory-cart-api.md` y `docs/api-contract.md` actualizados. Tests en `InventoryControllerTest` e `InventoryApiIT` (`%3Cb%3Ex`). | `35516b5` |
| A07 | BAJA | Corregir | `TEXT = "[^\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]*"`. Casos en `OrderControllerTest`: `‮` en `firstName`, ` ` en `lastName` y `​` en `extraPickups` → 400; y un caso positivo con tildes, eñes, griego y chino → 201. El HTML se sigue guardando tal cual (lo escapa Angular). | `ecc0752` |
| A08 | BAJA | Corregir | `JsonCoercionConfiguration` (y la nueva `InputPatterns`, A06) registradas en el principio II de la constitución 1.3.0, una línea en la ADR 0011, `shared/package-info.java` y `data-model.md` §1. | `c60d771` |
| A09 | BAJA | Corregir | `spec.md` Status → Implemented; `api-contract.md:83`; ADR 0011 (`decreaseStock` en :69/:73, "Aplicado tras aceptarse" en lugar de "Pendiente"); `quickstart.md` §7 marcado como diferido en parte, sin `order.api.OrderPlaced` y con una prueba de FR-026 comprobada (falla `ModularityTests` con `inventory → catalog :: api`); `plan.md:142` → `findAllContainingSkus`; `tasks.md` T020 → `MANDATORY` y T076 marcado; `contexto-sesion.md` con el estado tras el merge. | `dcd2a92` |
| A10 | BAJA | Diferido (no bloquea) | PR corto de cierre de la 003 el 2026-11-27 con los puntos 2 y 4–7 de la aprobación del PR #3 y el `diff` del ruleset; anotado en el backlog y en `contexto-sesion.md`. | — |
| A11 | BAJA | Diferido (ya hecho en otra rama) | Fase 5 completa (fixtures negativas, T061 y comentario de `ArchitectureRules`) en la rama `004-eventos-pedidos` (`9d5d9f3`); se integra tras este merge, 2026-11-30. No se toca aquí. | — |
| A12 | NIT | Corregir | `OrderConcurrencyIT`: latch `ready` de 2 (`ready.await(5, SECONDS)` antes de abrir) y `get(10, SECONDS)`. El IT opcional de líneas cruzadas no se añade (lo cubre `stockIsDecreasedPerLineSortedBySku`). | `43d2929` |
| A13 | NIT | Corregir | `OrderTestData.restoreSeed` reaplica el `INSERT` de `V4__inventory_stock.sql`, como el script. | `1cb8c4d` |
| A14 | NIT | Corregir | `FindVariantsService.find`: lista vacía → `List.of()` sin consulta; consulta con `requested` (deduplicado). Tests de vacío y de duplicados. | `a5af1a6` |
| A15 | NIT | Corregir | Javadoc de `StockResponse` → `InventoryStatus`; `getOfAnotherSessionIsOrderNotFound` usa `ORDER_ID` (entró en el commit de A07); casos de `requireText` en blanco/nulo y de `OrderId` nulo. | `015d5bd` (y `ecc0752`) |
| E1, E2 | ELOGIO | — | Sin cambios. | — |
