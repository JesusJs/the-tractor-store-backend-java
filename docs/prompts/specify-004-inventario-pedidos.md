Funcionalidad: **inventario y pedidos** (una sola spec; fusiona las filas 006 `inventario`, 007
`carrito-sesion` y 008 `pedidos-eventos` del backlog, ya reducidas por la ADR 0011).

Qué y por qué:
- Como visitante quiero ver si una variante tiene stock y confirmar mi pedido sin registrarme,
  y ver después el recibo con subtotal, IVA y total, para recogerlo en una tienda.
- Como frontend (ya implementado, no cambia) necesito que inventario, carrito y pedidos respondan
  con la forma que consumo hoy y que el backend cree la cookie de sesión.
- Como equipo queremos que precio, IVA y stock sean autoridad del backend, que el descuento de
  stock sea atómico y que el patrón de eventos con outbox quede demostrado.

Fuente de verdad (en este orden; léelas antes de escribir la spec):
1. `docs/adr/0011-contrato-carrito-y-pedidos-segun-front.md` (aceptada): decisiones de contrato,
   IVA, stock síncrono, sesión en `shared`, dependencias y deuda aceptada.
2. `docs/api-contract.md`, secciones Convenciones, Inventory, Cart, Order y Modelos (ya reescritas).
3. El front: `packages/shared-catalog/src/lib/cart.service.ts`, `models/catalog.models.ts`,
   `product.service.ts`, `tokens.ts` y `packages/mfe-checkout/src/page/{checkout,thanks}`.
   No inventes campos: si algo no cuadra, márcalo `[NEEDS CLARIFICATION]`.

Hallazgos del front (verificados, no los cambies):
- `GET /api/v1/inventory/{sku}` → lee `{ sku, stock }`: `stock > 0` → "N in stock", `0` → "Out of stock".
- `GET /api/v1/cart` con `withCredentials: true` y `responseType: 'text'`; ignora cuerpo y errores.
  El carrito vive en `localStorage`.
- `POST /api/v1/orders` envía `{ firstName, lastName, storeId, extraPickups?, items[] }`;
  `extraPickups` llega como string `"a, b"` o no llega; cada ítem trae `variantId` (= SKU, o `''`
  si el front no lo encontró en el catálogo), `quantity` y campos que el backend ignora
  (`price` "desde", `productId`, `productName`, `variantName`, `image`). Lee `receipt.id` y navega
  a `/thanks?orderId=…`.
- `GET /api/v1/orders/{id}` → pinta `OrderReceipt`; hace `o.extraPickups.length` (nunca `null`).
- El interceptor del front convierte cualquier error HTTP en un `Error` genérico: los códigos sirven
  para el contrato y los tests, no para la UI.

## Historias priorizadas

### P1 — Stock y sesión (MVP independiente)
- `GET /api/v1/inventory/{sku}` → 200 `{ sku, stock }`. SKU desconocido → 404 `VARIANT_NOT_FOUND`;
  SKU > 20 caracteres → 400 `VALIDATION_ERROR`. Sin sesión.
- Stock sembrado por migración Flyway (tabla `inventory_stock`, un valor por cada SKU del dataset;
  **al menos un SKU con stock 0**). Migraciones a partir de V4 (comprueba que la rama 003, aún sin
  mergear, no use ya ese número).
- `GET /api/v1/cart` → 200 con un `Cart` vacío con la forma del front:
  `{ items: [], totalItems: 0, subTotal: 0, tax: 0, total: 0 }`. Crea `ts_session` si falta o si
  el valor recibido no es un UUID válido (se sustituye; nunca se confía en él).
- Cookie `ts_session` (ADR 0004): UUID aleatorio, `HttpOnly`, `SameSite=Lax`, `Secure` en prod,
  `Path=/`. El componente que la lee o crea es técnico y vive en `shared.infrastructure.web`
  (ADR 0009 ampliada por la 0011); lo reutiliza P2.
- Criterio de demo: la ficha del front muestra "N in stock" y "Out of stock" según el SKU, y la
  cookie aparece tras abrir el carrito.

### P2 — Confirmar y consultar pedido
- `POST /api/v1/orders` → **201** + `Location: /api/v1/orders/{id}` + `OrderReceipt` en el cuerpo.
  Crea `ts_session` si falta y liga el pedido a ella.
- De cada línea solo se usan `variantId` y `quantity`; precio real de la variante, nombres e imagen
  salen de `catalog.api` (nueva interfaz pública `CatalogApi` con un record de variante: sku,
  productId, productName, variantName, price, image). Regresión .NET: un `price` manipulado en el
  body no cambia el `subTotal`.
- `firstName`, `lastName`, `storeId` se guardan tal cual. `extraPickups` es texto libre: se guarda
  tal cual y el recibo lo devuelve partido por `", "`; `[]` si no llegó.
- `subTotal` = Σ precio × cantidad; `tax` = `subTotal × 0.21` redondeado a 2 decimales
  `RoundingMode.HALF_UP`; `total` = `subTotal + tax`. `BigDecimal` / `NUMERIC(10,2)`.
- Stock síncrono vía `inventory.api` (interfaz `InventoryApi`):
  - Descuento **condicional y atómico en BD**:
    `UPDATE inventory_stock SET stock = stock - :qty WHERE sku = :sku AND stock >= :qty`;
    0 filas → 409 `OUT_OF_STOCK`. Nunca leer-y-luego-escribir.
  - La transacción la abre el caso de uso de `order.application`; si una línea de un pedido de
    varias falla, se revierte todo (ni pedido ni descuentos).
- Errores: `items` vacío → 409 `CART_EMPTY`; `variantId` vacío → 400 `VALIDATION_ERROR`
  (`@NotBlank`); SKU con formato válido inexistente → 404 `VARIANT_NOT_FOUND`; `quantity < 1` →
  400 `VALIDATION_ERROR`; stock insuficiente → 409 `OUT_OF_STOCK`. Todos con el formato único y
  `traceId`.
- `GET /api/v1/orders/{id}` → `OrderReceipt` (`status: 'PLACED'`, `placedAt` ISO-8601); pedido de
  otra sesión o inexistente → 404 `ORDER_NOT_FOUND`. Regresión .NET: el id devuelto por el POST se
  puede consultar con la misma cookie; las líneas llevan el SKU en `variantId`.
- Deuda aceptada (documentarla, no arreglarla): el carrito del front usa el precio "desde" y el
  recibo el real (`AU-07-YE`: 4000 en carrito, 5000 en recibo).
- Criterio de demo: ficha → carrito → checkout → página de gracias con el recibo correcto.

### P3 — Evento `OrderPlaced` con outbox
- `order` publica `OrderPlaced` (record en `order.api`: orderId, sessionId, líneas sku + quantity)
  desde el caso de uso transaccional.
- Añadir `spring-modulith-starter-jpa` y la migración Flyway con las tablas del Event Publication
  Registry copiando el DDL PostgreSQL de la versión exacta de Modulith del `pom.xml` (2.1.1; ver
  `specs/001-esqueleto-modular/research.md` R2). Sin inicialización automática de esquema.
- **Solo `notifications` escucha** (`@ApplicationModuleListener`) y registra en log "email enviado"
  con el id del pedido. `inventory` no escucha `OrderPlaced` (ADR 0011: evitaría el ciclo
  order ↔ inventory; el stock ya se descontó en P2).
- Tests con `Scenario` de Spring Modulith y un test de reintento con un listener que falla una vez.

## Arquitectura (skills `hexagonal-architecture`, `modular-architecture`, `testing-strategy`)
- `inventory`, `cart` y `order` con `domain` (Java puro, puertos de salida en `domain.port.out`),
  `application` (casos de uso en `application.port.in`), `infrastructure/web|persistence|config` y
  `api` (`@NamedInterface("api")`). Controllers solo llaman casos de uso.
- Dependencias (ya declaradas en los `package-info.java`): `order → catalog :: api, inventory :: api,
  shared`; `cart → shared`; `inventory → shared`; `notifications → order :: api`. Sin ciclos.
- Tablas `inventory_*` y `order_*`, sin FKs ni JOINs a tablas de otros módulos. `cart` no tiene tablas.
- Prueba primero, de dentro hacia fuera. Tests clave:
  - Unitario de dominio del IVA con casos de redondeo (p. ej. subtotal 0,05 → tax 0,01).
  - `@WebMvcTest` con el body exacto que envía `cart.service.ts` (`extraPickups` como string) y la
    forma JSON de `OrderReceipt`.
  - Integración con Testcontainers: cookie creada en `GET /cart` y `POST /orders`; otra sesión → 404;
    pedido de varias líneas con la última sin stock → 409 sin pedido ni descuentos; **concurrencia**:
    dos pedidos simultáneos por el último ejemplar → uno 201 y otro 409, stock final 0.

## Deuda de arquitectura que entra en esta spec (cada regla con su fixture negativa en `ArchitectureRulesSelfTests`)
- Review 002 #3: `tablesArePrefixedWithModule` debe fallar si un `@ElementCollection` o
  `@ManyToMany` no declara `@CollectionTable`/`@JoinTable` explícito, y validar también `@SecondaryTable`.
- Review 002 #12: `domain` no puede depender de `jakarta..` ni `io.swagger..`; la regla de puertos
  pasa de "`*Repository` en `domain.port.out`" a "toda interfaz de `domain` que sea puerto de salida
  vive en `domain.port.out`" (un `*Gateway` fuera de ahí debe fallar).
- Review 001 #15: `domain` no depende de `application`; `application` solo usa de Spring
  `@Service`/`@Transactional`; `@Transactional` solo en `application` (nunca en controllers ni
  adaptadores).

Fuera de alcance: carrito en servidor (`/cart/mini`, `POST`/`DELETE /cart/items`), reserva de stock,
reposición, emails reales, validar `extraPickups` contra tiendas, caché, cambios en el front.
