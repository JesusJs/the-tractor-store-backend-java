# 0011 — El contrato de carrito y pedidos lo manda el front

- Estado: aceptada (aprobada 2026-11-24; enmendada el mismo día en la spec 004: `storeId` se valida contra el catálogo). **La parte "sin carrito en servidor" es una desviación temporal** por decisión del mentor (review del PR #5, A01, 2026-11-26): la cierra la spec 005 `carrito-servidor` (2026-12-01), con los 4 endpoints de la guía ligados a `ts_session`, precio real de la variante y stock, y el `CartService` del front consumiéndolos ([issue del front](https://github.com/JesusJs/the-tractore-store/issues/1)). Registrada en `docs/requisitos.md` y `docs/backlog.md`.
- Fecha: 2026-11-24
- Reemplaza: ADR 0007. Amplía: ADR 0009 (sesión en `shared`). Modifica: ADR 0002 (consumidores de `OrderPlaced`).

## Contexto
Se revisó el front Angular (`packages/shared-catalog/src/lib/cart.service.ts`, `models/catalog.models.ts` y `packages/mfe-checkout/src/page/{cart,checkout,thanks}`). El carrito **vive en `localStorage`** (clave `tractor_cart`) y el front solo llama a cuatro endpoints:

| Llamada | Para qué | Qué hace el front |
|---|---|---|
| `GET /cart` | Que el backend ponga la cookie de sesión | `responseType: 'text'`; ignora el cuerpo y cualquier error |
| `GET /inventory/{sku}` | Stock en la ficha | Lee `{ sku, stock }` |
| `POST /orders` | Confirmar pedido | Envía `{ firstName, lastName, storeId, extraPickups?, items[] }`; lee `receipt.id` |
| `GET /orders/{id}` | Página de gracias (`?orderId=` en la URL) | Pinta un `OrderReceipt` |

Hechos comprobados en el código del front:
- **Identificador de línea**: `items[].variantId` es el SKU de la variante (`addToCart(sku)` lo guarda ahí). Si el SKU ya no aparece en el catálogo, `hydrateCart` deja `variantName = ''` y `placeOrder` envía `variantId: ''`.
- Cada línea lleva además `productId`, `productName`, `variantName` (= SKU), `price`, `quantity` e `image`. **`price` es el precio "desde" del producto** (`product.price`, mínimo de variantes, ADR 0008), no el de la variante.
- **`extraPickups` no llega como array**: `placeOrder` lo convierte en un string unido con `", "` o lo omite si está vacío, aunque el tipo `OrderPayload` diga `string[]`.
- `OrderReceipt` = `{ id, firstName, lastName, storeId, extraPickups: string[], items: OrderReceiptItem[], subTotal, tax, total, placedAt, status }`; los ítems tienen la misma forma que los del payload. La página de gracias hace `o.extraPickups.length`, así que nunca puede ser `null`.
- **IVA**: el front calcula `tax = subTotal * 0.21` y `total = subTotal + tax` en `number` de JavaScript, **sin redondear**. El redondeo solo ocurre al pintar, con `Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })`: 2 decimales, mitades hacia arriba (`halfExpand`).
- El front no lee la cookie (es HttpOnly); el comentario que la llama `tractor_session` no afecta al nombre real `ts_session`.

`docs/api-contract.md` describía en cambio un carrito en servidor (4 endpoints), un `Order` con `lineItems`/`createdAt`, dinero "sin IVA" (P7) e inventario con `inStock`; y la ADR 0007 hacía que `order` vaciara el carrito por `cart.api`. Nada de eso lo usa el front, que no cambia.

Además, para responder 409 `OUT_OF_STOCK` al confirmar, `order` tiene que consultar `inventory.api`. Si `inventory` siguiera escuchando `OrderPlaced` (publicado en `order.api`, ADR 0002) habría un ciclo `order ↔ inventory`, que `ApplicationModules.verify()` rechaza (mismo problema que resolvió la ADR 0007 con `cart`).

## Decisión
Como en la ADR 0008, **el front manda**: las secciones Inventory, Cart y Order del contrato se reescriben con la forma que consume el front y **no hay carrito en servidor** por ahora (diferido).

- **`GET /api/v1/inventory/{sku}`** → `{ sku, stock }` (sin `inStock`).
- **`GET /api/v1/cart`**: solo garantiza la cookie `ts_session` (la crea si no existe) y responde 200 con un carrito vacío con la forma `Cart` del front. `GET /cart/mini`, `POST /cart/items` y `DELETE /cart/items/{sku}` salen del contrato.
- **`POST /api/v1/orders`** → **201** + `Location: /api/v1/orders/{id}` + `OrderReceipt` en el cuerpo (el front lee `receipt.id`).
  - De cada línea usa **solo `variantId` (SKU) y `quantity`**; ignora `price`, `productId`, `productName`, `variantName` e `image` que envía el cliente y los obtiene de `catalog.api` (precio real de la variante).
  - Guarda `firstName` y `lastName` tal cual llegan del cliente. **`storeId` se valida** contra las tiendas del catálogo con un método de existencia de tienda de `catalog.api` y, si existe, se guarda tal cual; si no → 404 `STORE_NOT_FOUND` (enmienda del 2026-11-24, spec 004 FR-011: el backend no se fía del validador del formulario del front).
  - **`extraPickups` es texto libre**: se guarda tal cual; el recibo lo devuelve como lista partiendo por `", "`, y `[]` si no llegó. Nunca `null`.
  - Crea la cookie `ts_session` si no existe y liga el pedido a ella.
  - Errores: `items` vacío → 409 `CART_EMPTY`; `variantId` vacío → 400 `VALIDATION_ERROR` (`@NotBlank`); `storeId` que no es una tienda del catálogo → 404 `STORE_NOT_FOUND`; SKU con formato válido inexistente → 404 `VARIANT_NOT_FOUND`; `quantity < 1` → 400 `VALIDATION_ERROR`; stock insuficiente → 409 `OUT_OF_STOCK`.
- **`GET /api/v1/orders/{id}`** → `OrderReceipt`; pedido de otra sesión o inexistente → 404 `ORDER_NOT_FOUND`.
- **IVA del 21 %** calculado en el backend: `tax = subTotal × 0,21` redondeado a 2 decimales con `RoundingMode.HALF_UP` (equivale al `halfExpand` con que el front pinta importes positivos); `total = subTotal + tax`. Deja sin efecto el "sin IVA" (P7) del contrato.
- **Stock síncrono**: el caso de uso de confirmar pedido valida **y descuenta** el stock llamando a `inventory.api`, en la misma transacción que guarda el pedido. `inventory` sale de los listeners de `OrderPlaced`; el único consumidor es `notifications` (log "email enviado"), vía Event Publication Registry (ADR 0002).
  - **Descuento condicional y atómico en BD**: `UPDATE inventory_stock SET stock = stock - :qty WHERE sku = :sku AND stock >= :qty`. Si afecta 0 filas → 409 `OUT_OF_STOCK`. Nunca leer-y-luego-escribir: así dos pedidos simultáneos sobre el mismo SKU no pueden dejar el stock negativo.
  - **Todo o nada**: la transacción la abre el caso de uso en `application` (`@Transactional`). Si una línea de un pedido de varias falla, se revierten los descuentos ya hechos y el pedido no se guarda.
- **Tradeoff aceptado**: el inventario pasa a ser síncrono a cambio de atomicidad (sin sobreventa ni compensaciones) y de no tener ciclo entre módulos. El patrón de eventos con outbox (constitución V) queda demostrado con `notifications`.
- **Sesión en `shared`**: el componente que lee o crea la cookie `ts_session` es técnico y transversal (lo usan `cart` y `order`) y vive en `shared.infrastructure.web`. Amplía la ADR 0009.
- **Dependencias**:
  - `order → catalog :: api, inventory :: api, shared` (ya no `cart :: api`).
  - `cart → shared` (con el carrito vacío no necesita `catalog` ni `inventory`).
  - `inventory → shared` (ya no `order :: api`).
  - `notifications → order :: api` (sin cambios).

## Alternativas consideradas
- **Carrito en servidor** (el contrato anterior): el front no lo llama; para usarlo habría que cambiar el checkout del front, y el front no cambia.
- **Carrito en servidor además del de `localStorage`**: nadie lo consumiría y no hay tiempo antes de la demo (principio VII, YAGNI).
- **Aceptar el precio que envía el front**: viola la constitución IV y el front envía el precio "desde", no el de la variante.
- **Descontar stock escuchando `OrderPlaced`**: o bien crea el ciclo `order ↔ inventory`, o bien obliga a que `order` no valide stock (sin 409 y con riesgo de stock negativo, porque el listener es asíncrono).
- **Cookie duplicada en `cart` y `order`**: dos implementaciones de una regla de seguridad (ADR 0004) que tienen que coincidir.

## Consecuencias
- Positivas:
  - El checkout del front funciona sin cambios contra el backend Java.
  - Precio, IVA y stock siguen siendo autoridad del backend.
  - Confirmar pedido es atómico: pedido guardado y stock descontado en la misma transacción, o ninguna de las dos cosas; no hay sobreventa.
  - Las dependencias quedan sin ciclos y `cart` se reduce a un endpoint.
- Negativas / deuda aceptada:
  - **Totales distintos entre carrito y recibo** cuando una variante cuesta más que el precio "desde": el carrito del front usa `product.price` (ADR 0008) y el recibo el precio real. Ejemplo: `AU-07-YE` aparece a 4000 en el carrito y a 5000 en el recibo (IVA y total cambian en proporción). Se acepta para la demo; arreglarlo exige cambiar el front o un carrito en servidor.
  - El carrito no sobrevive a otro navegador ni se valida en servidor hasta que se confirma el pedido.
  - `order` queda acoplado de forma síncrona a `inventory.api` (consultar y descontar); si el descuento falla, se deshace el pedido. Separar inventario en otro servicio obligaría a rehacer esto con eventos y compensaciones.
  - `inventory` deja de ser ejemplo de consumidor de eventos; solo queda `notifications`.
  - El backend acepta en el body campos que ignora (`price`, `productName`, …).
  - `extraPickups` es texto sin validar contra tiendas; un nombre con `", "` dentro se partirá en dos en el recibo.
  - `catalog.api` e `inventory.api` necesitan operaciones públicas nuevas (variante por SKU con precio, nombre e imagen; existencia de tienda por id; consultar y descontar stock).
- Cómo se verifica:
  - `ModularityTests`: `allowedDependencies` según la lista de arriba; un listener de `OrderPlaced` en `inventory` rompe el build.
  - Test unitario de dominio del IVA con casos de redondeo (p. ej. subtotal 0,05 → tax 0,01).
  - Test de regresión "precio enviado por el cliente": un `price` manipulado en el body no cambia el `subTotal` (constitución IV).
  - `@WebMvcTest` con el body exacto que envía `cart.service.ts` (`extraPickups` como string) y con la forma JSON de `OrderReceipt` (`extraPickups: []` cuando falta).
  - Test de integración (Testcontainers): `POST /orders` sin cookie la crea; `GET /orders/{id}` con otra cookie → 404; sin stock → 409 y ni pedido ni descuento.
  - Test de `POST /orders` con un `storeId` inexistente: 404 `STORE_NOT_FOUND`, sin pedido ni descuentos.
  - Test de integración de pedido con varias líneas donde la última no tiene stock: 409, ningún descuento aplicado y ningún pedido guardado.
  - Test de concurrencia (Testcontainers): dos pedidos simultáneos por el último ejemplar del mismo SKU → uno 201 y otro 409; el stock queda en 0, nunca negativo.
  - Validación manual con el front real: ficha → carrito → checkout → página de gracias.

## Pendiente tras aceptarse
- Enmienda MINOR de la constitución con `/speckit-constitution`:
  - **II**: `shared` admite también la sesión por cookie.
  - **IV**: "el cliente nunca envía precios" → "el backend ignora cualquier precio que envíe el cliente".
  - **V**: la razón ya no incluye desacoplar `inventory` (solo `notifications`).
  - **VI**: la cookie liga los pedidos; el carrito no está en servidor.
