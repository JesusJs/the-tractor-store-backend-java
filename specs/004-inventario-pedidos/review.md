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
