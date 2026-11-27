# Feature Specification: Inventario y pedidos (stock, sesión, pedido y evento `OrderPlaced`)

**Feature Branch**: `004-inventario-pedidos`

**Created**: 2026-11-24

**Status**: Implemented (P1 y P2 fusionadas en `main` el 2026-11-26 con el PR #5; review en `review.md`. La fase 5, la P3 y el pulido, diferidos en la review 004, se completaron el 2026-11-26 en la rama `004-eventos-pedidos`, rebasada sobre `main`, con su `/review-spec` (C1–C12) hecha; pendiente de PR)

**Input**: User description: "@docs/prompts/specify-004-inventario-pedidos.md — Funcionalidad inventario y pedidos (una sola spec; fusiona 006 `inventario`, 007 `carrito-sesion` y 008 `pedidos-eventos` del backlog, reducidas por la ADR 0011). Como visitante quiero ver si una variante tiene stock y confirmar mi pedido sin registrarme, y ver después el recibo con subtotal, IVA y total, para recogerlo en una tienda. Como frontend (ya implementado, no cambia) necesito que inventario, carrito y pedidos respondan con la forma que consumo hoy y que el backend cree la cookie de sesión. Como equipo queremos que precio, IVA y stock sean autoridad del backend, que el descuento de stock sea atómico y que el patrón de eventos con outbox quede demostrado. P1 stock y sesión; P2 confirmar y consultar pedido; P3 evento `OrderPlaced` con outbox. Incluye la deuda de arquitectura de las reviews 001 #15 y 002 #3 y #12. Fuera de alcance: carrito en servidor, reserva de stock, reposición, emails reales, validar `extraPickups` contra tiendas, caché, cambios en el front."

## Clarifications

### Session 2026-11-24

- Q: ¿`POST /orders` valida `storeId` contra las tiendas del catálogo o lo guarda tal cual (ADR
  0011)? → A: Se valida: un `storeId` que no es el id de una tienda del catálogo → 404
  `STORE_NOT_FOUND`, sin pedido ni descuentos, comprobado con un método de existencia de tienda
  de `catalog.api`. Enmienda la ADR 0011 (FR-011a). `extraPickups` sigue sin validarse.
- Q: ¿Se aceptan los supuestos tomados al especificar? → A: Sí: id de pedido UUID (id no UUID →
  404), límites de entrada de la sección Assumptions, orden de precedencia de errores y líneas
  duplicadas del mismo SKU procesadas como líneas independientes.

## Contexto: el front manda (ADR 0011)

Fuentes, en este orden: `docs/adr/0011-contrato-carrito-y-pedidos-segun-front.md` (aceptada),
`docs/api-contract.md` (secciones Convenciones, Inventory, Cart, Order y Modelos, ya reescritas) y
el código del front (`packages/shared-catalog/src/lib/cart.service.ts`,
`packages/shared-catalog/src/models/catalog.models.ts`, `packages/mfe-checkout/src/page/{checkout,thanks}`).

Hechos comprobados en el front (no cambian):

| Llamada | Qué envía el front | Qué hace con la respuesta |
|---|---|---|
| `GET /api/v1/inventory/{sku}` | — | Lee `{ sku, stock }`: `stock > 0` → "N in stock"; `0` → "Out of stock" |
| `GET /api/v1/cart` | Credenciales (`withCredentials`), `responseType: 'text'` | Ignora el cuerpo y cualquier error; solo sirve para que el navegador reciba la cookie. El carrito vive en `localStorage` (`tractor_cart`) |
| `POST /api/v1/orders` | `{ firstName, lastName, storeId, extraPickups?, items[] }`; `extraPickups` llega como texto `"a, b"` o no llega; cada ítem trae `variantId` (= SKU, o `''` si el front no lo encontró en el catálogo), `quantity` y además `price` (precio "desde"), `productId`, `productName`, `variantName`, `image` | Lee `receipt.id` y navega a `/thanks?orderId=…` |
| `GET /api/v1/orders/{id}` | Credenciales | Pinta el recibo; usa `o.extraPickups.length` (nunca puede ser `null`) |

- El interceptor del front convierte cualquier error HTTP en un error genérico: los códigos de
  error sirven para el contrato y los tests, no para la interfaz.
- El front valida en el formulario: nombre y apellido obligatorios (mínimo 2 caracteres), tienda
  obligatoria elegida de la lista de tiendas, y cada recogida extra con mínimo 2 caracteres.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver el stock de una variante y obtener sesión (Priority: P1)

Un visitante abre la ficha de un tractor y, para la variante elegida, ve cuántas unidades quedan
("N in stock") o que está agotada ("Out of stock"). Al abrir el carrito, el navegador recibe una
cookie de sesión anónima que después liga sus pedidos, sin registrarse.

**Why this priority**: es el primer paso del recorrido de compra de la demo y la base de P2 (el
stock que se descuenta y la sesión que liga el pedido). Se puede demostrar sola.

**Independent Test**: con la BD migrada desde vacío, se pide el stock de un SKU con unidades, de
uno agotado y de uno inexistente; se pide el carrito sin cookie y con una cookie inválida. Con el
front sin cambios, la ficha muestra "N in stock" o "Out of stock" según el SKU, y la cookie
aparece en el navegador tras abrir el carrito.

**Acceptance Scenarios**:

1. **Given** un SKU del catálogo con unidades, **When** se pide su stock, **Then** se recibe 200
   `{ sku, stock }` con `stock > 0`.
2. **Given** el SKU sembrado con stock 0, **When** se pide su stock, **Then** se recibe 200 con
   `stock = 0`.
3. **Given** un SKU que no existe, **When** se pide su stock, **Then** se recibe 404
   `VARIANT_NOT_FOUND` con el formato de error único.
4. **Given** un SKU de más de 20 caracteres, **When** se pide su stock, **Then** se recibe 400
   `VALIDATION_ERROR`.
5. **Given** una petición sin cookie de sesión, **When** se pide el carrito, **Then** se recibe
   200 `{ items: [], totalItems: 0, subTotal: 0, tax: 0, total: 0 }` y la respuesta fija una
   cookie `ts_session` nueva (UUID aleatorio, HttpOnly, `SameSite=Lax`, `Path=/`, `Secure` en
   producción).
6. **Given** una cookie `ts_session` con un UUID válido, **When** se pide el carrito, **Then** se
   recibe el carrito vacío y la cookie no se sustituye.
7. **Given** una cookie `ts_session` cuyo valor no es un UUID válido, **When** se pide el carrito,
   **Then** se ignora ese valor y se fija una cookie nueva con un UUID generado por el backend.

---

### User Story 2 - Confirmar el pedido y consultar el recibo (Priority: P2)

Desde el checkout, el visitante escribe nombre y apellido, elige la tienda de recogida (y, si
quiere, recogidas extra) y confirma. El backend calcula el importe con los precios reales del
catálogo y el IVA, descuenta el stock y devuelve el recibo; el visitante llega a la página de
gracias y ve sus líneas, subtotal, IVA (21 %) y total. Nadie más puede ver ese pedido.

**Why this priority**: es el cierre de la demo (ficha → carrito → checkout → gracias) y donde
están las reglas de negocio con más riesgo: precio autoritativo, IVA, stock sin sobreventa y
aislamiento por sesión.

**Independent Test**: se confirma un pedido con el cuerpo exacto que envía el front y se
comprueba el 201, la cabecera `Location`, la forma del recibo, los importes y el stock
descontado; se consulta con la misma cookie (200) y con otra (404); se prueban los errores y dos
pedidos simultáneos por el último ejemplar.

**Acceptance Scenarios**:

1. **Given** SKUs con stock suficiente y una petición sin cookie, **When** se confirma un pedido
   con el cuerpo que envía el front, **Then** se recibe 201, la cabecera
   `Location: /api/v1/orders/{id}`, una cookie `ts_session` nueva y un recibo `{ id, firstName,
   lastName, storeId, extraPickups, items, subTotal, tax, total, placedAt, status }` con
   `status = "PLACED"`, y el stock de cada SKU baja en la cantidad pedida.
2. **Given** una línea con `variantId = "AU-07-YE"` y `price = 4000` (precio "desde" que envía el
   front), **When** se confirma el pedido, **Then** la línea del recibo tiene `price = 5000`
   (precio real de la variante) y el `subTotal` se calcula con 5000: un precio manipulado en el
   cuerpo nunca cambia el importe (regresión .NET).
3. **Given** un pedido confirmado, **When** se consulta su recibo con la misma cookie, **Then**
   se recibe 200 con el mismo recibo, y cada línea lleva el SKU en `variantId` y en `variantName`
   (regresiones .NET: id consultable y líneas con SKU en vez de nombre).
4. **Given** un pedido confirmado, **When** se consulta con otra cookie, sin cookie o con un id
   que no existe, **Then** se recibe 404 `ORDER_NOT_FOUND` (nunca 403).
5. **Given** `extraPickups = "Store A, Store B"`, **When** se confirma el pedido, **Then** el
   recibo devuelve `extraPickups = ["Store A", "Store B"]`; **Given** que `extraPickups` no llega,
   **Then** el recibo devuelve `extraPickups = []`.
6. **Given** un subtotal de 0,05, **When** se calcula el IVA, **Then** `tax = 0,01` y
   `total = 0,06` (21 % redondeado a 2 decimales, mitades hacia arriba).
7. **Given** `items` vacío o ausente, **When** se confirma, **Then** se recibe 409 `CART_EMPTY` y
   no se guarda nada.
8. **Given** una línea con `variantId = ""` o con `quantity < 1`, **When** se confirma, **Then**
   se recibe 400 `VALIDATION_ERROR` con el campo en `details` y no se guarda nada.
9. **Given** una línea con un SKU de formato válido que no existe en el catálogo, **When** se
   confirma, **Then** se recibe 404 `VARIANT_NOT_FOUND` y no se guarda nada.
10. **Given** un `storeId` que no es el id de ninguna tienda del catálogo, **When** se confirma,
    **Then** se recibe 404 `STORE_NOT_FOUND`, no se guarda el pedido y no se descuenta stock.
11. **Given** un pedido de varias líneas cuya última línea pide más unidades de las que hay,
    **When** se confirma, **Then** se recibe 409 `OUT_OF_STOCK`, no se guarda el pedido y el
    stock de **todas** las líneas queda como estaba.
12. **Given** un SKU con exactamente 1 unidad, **When** llegan dos pedidos simultáneos por esa
    unidad, **Then** uno recibe 201 y el otro 409 `OUT_OF_STOCK`, y el stock final es 0 (nunca
    negativo).

---

### User Story 3 - Aviso de pedido confirmado por evento con outbox (Priority: P3)

Cuando se confirma un pedido, el módulo de notificaciones se entera por un evento interno y
"envía" el email de confirmación (lo registra en el log con el id del pedido). Si el envío falla,
el evento no se pierde: queda registrado y se reintenta.

**Why this priority**: demuestra el patrón de eventos con outbox exigido por la constitución (V),
pero el recorrido de compra funciona sin él; es lo primero que se recorta si aprieta el plazo.

**Independent Test**: se confirma un pedido y se comprueba que se publica `OrderPlaced` con el id
del pedido, la sesión y sus líneas, y que el listener de notificaciones lo consume y deja la línea
de log; con un listener que falla una vez, el evento queda pendiente en el registro y se entrega
en el reintento.

**Acceptance Scenarios**:

1. **Given** un pedido que se confirma con éxito, **When** termina la confirmación, **Then** se
   publica un único `OrderPlaced { orderId, lines[sku, quantity] }` y notificaciones
   registra en el log "email enviado" con ese `orderId`.
2. **Given** una confirmación que falla (sin stock, SKU inexistente, validación), **When**
   termina, **Then** no se publica ningún `OrderPlaced`.
3. **Given** que el listener de notificaciones falla la primera vez, **When** se reintenta la
   entrega de las publicaciones pendientes, **Then** el evento se entrega y queda marcado como
   completado en el registro de publicaciones.
4. **Given** el inventario, **When** se confirma un pedido, **Then** el inventario no escucha
   `OrderPlaced` (el stock ya se descontó en la confirmación); un listener de ese evento en
   inventario rompe el build por ciclo entre módulos.

---

### Edge Cases

- Cuerpo de `POST /orders` con `extraPickups` como array (`["a","b"]`) en lugar de texto: el
  front nunca lo envía así; se trata como petición mal formada (400 `MALFORMED_REQUEST`) porque el
  contrato fija texto.
- `extraPickups` presente pero vacío o solo espacios (`""`): se trata como ausente → `[]` en el
  recibo.
- `extraPickups` con un nombre que contiene `", "`: se parte en dos en el recibo (deuda aceptada,
  ADR 0011).
- Dos líneas del mismo SKU en un pedido: se procesan como líneas independientes (cada una
  descuenta su cantidad); si entre las dos superan el stock, la segunda falla y se revierte todo.
- Varios errores a la vez (p. ej. un `variantId` vacío y `items` con otro SKU sin stock): los de
  validación (400) se devuelven antes de tocar catálogo o inventario.
- SKU que existe en el catálogo pero no tiene fila de stock (no debería ocurrir, el dataset de
  stock cubre todos): se responde 409 `OUT_OF_STOCK` y se revierte el pedido.
- `GET /orders/{id}` con un id que no tiene el formato de los identificadores de pedido: 404
  `ORDER_NOT_FOUND` (no se revela nada ni se distingue de "no existe").
- `GET /orders/{id}` sin cookie: 404 `ORDER_NOT_FOUND`; este endpoint no crea cookie.
- `POST /orders` con cookie `ts_session` inválida: se sustituye por una nueva y el pedido se liga
  a la nueva.
- `price` negativo, `productName` manipulado o `image` ajena en el cuerpo: se ignoran; el recibo
  usa siempre los datos del catálogo.
- Carrito del front con precio "desde" distinto del real (`AU-07-YE`: 4000 en carrito, 5000 en
  recibo): el recibo manda; deuda aceptada, no se corrige en esta spec.
- Base de datos caída durante la confirmación: 500 `INTERNAL_ERROR`, sin pedido ni descuentos.

## Requirements *(mandatory)*

### Functional Requirements

**Inventario (P1)**

- **FR-001**: `GET /api/v1/inventory/{sku}` MUST devolver 200 `{ sku, stock }` con el stock
  actual del SKU (entero ≥ 0). No usa ni crea la cookie de sesión.
- **FR-002**: SKU desconocido para el inventario → 404 `VARIANT_NOT_FOUND`; SKU de más de 20
  caracteres → 400 `VALIDATION_ERROR`.
- **FR-003**: El stock inicial MUST crearse con una migración versionada: una fila por cada SKU
  del dataset del catálogo y **al menos un SKU con stock 0**. Las migraciones nuevas MUST
  numerarse a partir de V4 (V1–V3 ya existen en `main` y en `003-calidad-sonar`).
- **FR-004**: El stock MUST pertenecer exclusivamente al módulo de inventario (tablas con prefijo
  `inventory_`, sin claves foráneas ni JOINs a tablas de otros módulos) y MUST NOT ser negativo.

**Sesión y carrito (P1)**

- **FR-005**: `GET /api/v1/cart` MUST devolver 200
  `{ items: [], totalItems: 0, subTotal: 0, tax: 0, total: 0 }` siempre (no hay carrito en
  servidor).
- **FR-006**: `GET /api/v1/cart` y `POST /api/v1/orders` MUST fijar la cookie `ts_session` cuando
  la petición no la trae o su valor no es un UUID válido; el valor nuevo es un UUID aleatorio
  generado por el backend (nunca se reutiliza un valor recibido no válido). Atributos: `HttpOnly`,
  `SameSite=Lax`, `Path=/` y `Secure` en el perfil de producción. Si la cookie trae un UUID
  válido, se respeta y no se reenvía.
- **FR-007**: La lectura y creación de la cookie de sesión MUST ser un componente técnico único
  del módulo compartido, reutilizado por carrito y pedidos (ADR 0009 ampliada por la 0011).

**Confirmar pedido (P2)**

- **FR-008**: `POST /api/v1/orders` MUST aceptar el cuerpo exacto que envía el front
  (`OrderPayload`, con `extraPickups` como texto opcional y campos de línea que se ignoran) y, si
  tiene éxito, responder **201** con la cabecera `Location: /api/v1/orders/{id}` y el
  `OrderReceipt` en el cuerpo.
- **FR-009**: De cada línea MUST usarse solo `variantId` (SKU) y `quantity`. `price`,
  `productId`, `productName`, `variantName` e `image` recibidos MUST ignorarse; en el recibo salen
  del catálogo: `productId`, `productName` (nombre del producto), `variantName` (= SKU), `price`
  (precio real de la variante, no el "desde") e `image` (imagen de la variante).
- **FR-010**: El catálogo MUST ofrecer a otros módulos, por su cara pública, la consulta de una
  variante por SKU con: sku, id y nombre de producto, nombre de variante, precio e imagen.
- **FR-011**: `firstName`, `lastName` y `storeId` MUST guardarse y devolverse tal cual llegan
  (validados como obligatorios). Además, `storeId` MUST corresponder a una tienda del catálogo
  (comparación exacta con su `id`); si no existe → 404 `STORE_NOT_FOUND` y no se guarda nada ni
  se descuenta stock. Para ello `catalog.api` MUST ofrecer un método público de existencia de
  tienda por id.
- **FR-011a**: Esta regla enmienda la ADR 0011 (antes "`storeId` tal cual"; enmienda del
  2026-11-24) y figura en los errores de `POST /orders` de `docs/api-contract.md`. El código
  `STORE_NOT_FOUND` ya estaba en la tabla de errores del contrato: no se crea ninguno nuevo.
- **FR-012**: `extraPickups` MUST guardarse como texto tal cual llega; el recibo lo devuelve como
  lista partiendo por `", "`, y `[]` (nunca `null`) si no llegó o está vacío.
- **FR-013**: Importes: `subTotal` = Σ (precio real × cantidad); `tax` = `subTotal × 0,21`
  redondeado a 2 decimales con mitades hacia arriba; `total` = `subTotal + tax`. Todos como
  decimales exactos de 2 decimales, nunca coma flotante.
- **FR-014**: El pedido MUST ligarse a la sesión de la cookie y guardarse con estado `PLACED` y la
  fecha/hora de confirmación (`placedAt`, ISO-8601).
- **FR-015**: Stock síncrono: la confirmación MUST validar y descontar el stock de cada línea por
  la cara pública del inventario, con un descuento **condicional y atómico** en la base de datos
  (solo descuenta si `stock ≥ cantidad`, en una única operación; nunca leer y luego escribir). Si
  no se descuenta → 409 `OUT_OF_STOCK`.
- **FR-016**: Todo o nada: guardar el pedido y descontar el stock de todas sus líneas MUST ocurrir
  en una única transacción abierta por el caso de uso de confirmar pedido; si cualquier línea
  falla, MUST revertirse todo (ni pedido ni descuentos).
- **FR-017**: Errores de `POST /orders`, todos con el formato único y `traceId`: `items` vacío o
  ausente → 409 `CART_EMPTY`; `variantId` vacío o de más de 20 caracteres, `quantity < 1`,
  `firstName`/`lastName`/`storeId` vacíos → 400 `VALIDATION_ERROR` con el campo en `details`;
  `storeId` que no es una tienda del catálogo → 404 `STORE_NOT_FOUND`; SKU
  de formato válido inexistente en el catálogo → 404 `VARIANT_NOT_FOUND`; stock insuficiente → 409
  `OUT_OF_STOCK`. Cuerpo ilegible o con tipos incorrectos → 400 `MALFORMED_REQUEST`.
- **FR-018**: Toda entrada (cuerpo y ruta) MUST validarse con límites de tamaño (constitución VI)
  para que una petición abusiva reciba 400 `VALIDATION_ERROR` y no llegue a la base de datos.

**Consultar pedido (P2)**

- **FR-019**: `GET /api/v1/orders/{id}` MUST devolver 200 con el `OrderReceipt` del pedido si
  pertenece a la sesión de la cookie; si no existe, es de otra sesión, no hay cookie o el id no
  tiene formato de identificador de pedido → 404 `ORDER_NOT_FOUND`.
- **FR-020**: El `id` devuelto por `POST /orders` MUST ser exactamente el que acepta
  `GET /orders/{id}` con la misma cookie (regresión .NET), y el recibo de la consulta MUST ser
  idéntico al de la confirmación.
- **FR-021**: Los datos del pedido MUST pertenecer exclusivamente al módulo de pedidos (tablas con
  prefijo `order_`, sin claves foráneas ni JOINs a tablas de catálogo o inventario); las líneas
  guardan la copia de SKU, nombres, precio e imagen del momento de la compra.

**Evento `OrderPlaced` (P3)**

- **FR-022**: Al confirmar un pedido con éxito MUST publicarse, dentro de la misma transacción,
  el evento `OrderPlaced { orderId, lines: [{ sku, quantity }] }`, definido en la cara
  pública del módulo de pedidos. Si la confirmación falla, no se publica.
- **FR-023**: La entrega MUST apoyarse en el registro de publicaciones de eventos (outbox) de
  Spring Modulith con su tabla creada por migración Flyway, copiando el DDL PostgreSQL de la
  versión exacta de Modulith del proyecto (2.1.1), sin inicialización automática de esquema.
- **FR-024**: El único consumidor de `OrderPlaced` MUST ser el módulo de notificaciones, que
  registra en el log "email enviado" con el id del pedido. Un fallo del consumidor MUST NOT
  deshacer el pedido, y el evento queda pendiente para reintento.
- **FR-025**: El inventario MUST NOT escuchar `OrderPlaced` (evitaría el ciclo pedidos ↔
  inventario).

**Arquitectura y dependencias**

- **FR-026**: Dependencias permitidas entre módulos: pedidos → caras públicas de catálogo e
  inventario y módulo compartido; carrito → compartido; inventario → compartido;
  notificaciones → cara pública de pedidos. Sin ciclos; cualquier otra dependencia rompe el build.
- **FR-027**: Inventario, carrito y pedidos MUST seguir las capas del proyecto (dominio puro,
  aplicación con casos de uso, adaptadores web/persistencia/config, cara pública), y los
  controllers MUST llamar solo a casos de uso.

**Guardas de arquitectura (deuda de las reviews 001 y 002; cada regla con su caso negativo en el
build)**

- **FR-028** (review 002 #3): La regla de prefijo de tablas MUST fallar si una colección o
  relación muchos-a-muchos persistente no declara explícitamente su tabla, y MUST validar también
  las tablas secundarias.
- **FR-029** (review 002 #12): El dominio MUST NOT depender de anotaciones de validación ni de
  documentación de API (`jakarta..`, `io.swagger..`); toda interfaz del dominio que sea puerto de
  salida MUST vivir en `domain.port.out` (un `*Gateway` fuera de ahí MUST fallar).
- **FR-030** (review 001 #15): El dominio MUST NOT depender de la capa de aplicación; la capa de
  aplicación MUST usar de Spring solo `@Service` y `@Transactional`; `@Transactional` MUST
  aparecer solo en la capa de aplicación (nunca en controllers ni adaptadores).

**Documentación**

- **FR-031**: Los 4 endpoints de esta spec MUST aparecer documentados (descripción, respuestas y
  errores) en la documentación interactiva de la API.

### Key Entities

- **StockItem** (inventario): SKU y unidades disponibles (entero ≥ 0). Sabe si puede servir una
  cantidad; el descuento se hace de forma condicional y atómica en la persistencia.
- **Session**: identificador anónimo (UUID) que viaja en la cookie `ts_session`; liga los pedidos.
  No se persiste como entidad propia.
- **Order** (pedidos): id, sesión, nombre, apellido, tienda, recogidas extra (texto), líneas,
  subtotal, IVA, total, estado (`PLACED`) y fecha de confirmación. Calcula sus importes.
- **OrderLine**: SKU, cantidad y copia de los datos del catálogo en el momento de la compra (id y
  nombre de producto, nombre de variante, precio unitario, imagen).
- **Money / Tax**: importe decimal exacto de 2 decimales; regla de IVA del 21 % con redondeo
  mitades hacia arriba.
- **VariantView** (cara pública del catálogo): sku, productId, productName, variantName, price,
  image.
- **OrderPlaced** (evento): orderId, líneas (sku, quantity). Sin `sessionId`: ningún consumidor lo usa
  y el evento se guarda en `event_publication` (minimización, review C5 del 2026-11-26).
- **Cart**: vista siempre vacía con la forma del front; sin persistencia.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con el front sin modificar apuntando al backend en local, el recorrido ficha →
  carrito → checkout → página de gracias se completa sin errores de red ni de consola, y la ficha
  muestra "N in stock" o "Out of stock" según el SKU.
- **SC-002**: En el 100 % de los pedidos de prueba, el subtotal, el IVA y el total del recibo
  coinciden con los calculados a mano con los precios reales del catálogo, incluidos los casos de
  redondeo y el de un precio manipulado en el cuerpo.
- **SC-003**: En una ejecución de pedidos simultáneos por el último ejemplar de un SKU, exactamente
  uno se confirma, el resto recibe "sin stock", y el stock nunca queda negativo.
- **SC-004**: En el 100 % de los pedidos rechazados (sin stock, SKU inexistente, tienda
  inexistente, validación, carrito vacío) no queda ningún pedido guardado ni ninguna unidad descontada.
- **SC-005**: El 100 % de los intentos de consultar un pedido desde otra sesión o sin sesión
  responden "no encontrado", sin revelar que existe.
- **SC-006**: El 100 % de los pedidos confirmados genera exactamente un aviso "email enviado" con
  su id, incluso cuando el primer intento de aviso falla.
- **SC-007**: Confirmar un pedido y cargar el recibo tarda menos de 1 segundo cada uno en local.
- **SC-008**: Cada nueva regla de arquitectura tiene un caso negativo que hace fallar el build.

## Assumptions

- **Contrato**: el de `docs/api-contract.md` y la ADR 0011 (aceptada); esta spec no añade campos ni
  códigos de error nuevos. `POST /orders` usa `STORE_NOT_FOUND` (ya en la tabla de errores) según la
  enmienda de la ADR 0011 (FR-011a).
- **Tiendas**: el front solo permite elegir tiendas de `GET /catalog/stores` (validador asíncrono
  del formulario), así que con el front real nunca se recibe un `storeId` inexistente; la
  validación protege contra clientes manipulados.
- **Identificador de pedido**: UUID generado por el backend; un id que no es UUID se responde como
  inexistente (404 `ORDER_NOT_FOUND`), porque el contrato no prevé un 400 en esa ruta.
- **Límites de entrada** (constitución VI; el contrato no los fija): `firstName` y `lastName`
  obligatorios y de hasta 100 caracteres; `storeId` obligatorio y de hasta 20; `extraPickups` de
  hasta 500; `variantId` de hasta 20; `quantity` entre 1 y 99; como máximo 50 líneas (aceptados
  el 2026-11-24); superarlos → 400 `VALIDATION_ERROR`. El backend no exige el
  mínimo de 2 caracteres del formulario del front.
- **Precedencia de errores**: primero la forma del cuerpo (400 `MALFORMED_REQUEST`), luego la
  validación (400 `VALIDATION_ERROR`), luego carrito vacío (409 `CART_EMPTY`), luego tienda (404
  `STORE_NOT_FOUND`), luego catálogo (404 `VARIANT_NOT_FOUND`) y por último stock (409
  `OUT_OF_STOCK`).
- **Stock sembrado**: valores enteros pequeños por SKU (los fija el plan); el SKU con stock 0 y uno
  con stock 1 (para el test de concurrencia en Testcontainers, que puede sembrar sus propios datos)
  se eligen en el plan. Sin reposición ni reservas.
- **`placedAt`**: instante de confirmación en UTC, formato ISO-8601.
- **Cookie**: sin `Max-Age` (cookie de sesión del navegador) salvo que el plan justifique otra
  cosa; el nombre real es `ts_session` aunque un comentario del front diga `tractor_session`.
- **Deuda aceptada (ADR 0011, se documenta, no se arregla)**: el carrito del front usa el precio
  "desde" y el recibo el real (`AU-07-YE`: 4000 en carrito, 5000 en recibo); el backend acepta
  campos del cuerpo que ignora; `extraPickups` no se valida contra tiendas.
- **Heredado de la constitución 1.3.0 (resuelto)**: el comentario de `shared/package-info.java`
  cita la sesión por cookie y, desde la review del PR #5 (A08), la validación técnica de entrada.
- **Dependencias**: catálogo de la spec 002 (dataset V3 con 47 SKUs y precios por variante),
  módulo compartido de errores/`traceId`/CORS, ADRs 0002, 0004, 0009 y 0011, constitución 1.3.0.
- **Fuera de alcance**: carrito en servidor (`/cart/mini`, `POST`/`DELETE /cart/items`), reserva de
  stock, reposición, emails reales, validar `extraPickups` contra tiendas, caché, cambios en el
  front.
