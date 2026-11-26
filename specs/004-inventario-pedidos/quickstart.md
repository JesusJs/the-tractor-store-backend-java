# Quickstart: validar 004 inventario y pedidos

Guía para comprobar de punta a punta que la funcionalidad cumple la spec. Las formas exactas están
en [contracts/](contracts/), y las tablas y el dataset en [data-model.md](data-model.md). Las
secciones siguen las prioridades: §2–§3 = P1, §4–§5 = P2 (objetivo de esta noche), §6–§7 = P3.

## Prerrequisitos

- Java 21, Docker en marcha, `jq` y `curl`.
- Rama `004-inventario-pedidos`.
- Para §5, el repo del front con dependencias instaladas: primero se arrancan los remotes y
  después el shell con `--skipRemotes` (Nx).

## 1. Pirámide de tests

```bash
./mvnw test
```

Esperado: verde. Incluye los tests de dominio (`StockItemTest`, `MoneyTest`, `VatTest`,
`OrderTest`, `OrderIdTest`), los `*ServiceTest`, `SessionCookiesTest`, `GlobalExceptionHandlerTest`
(con el 409), los `*ControllerTest`, `ModularityTests`, `HexagonalArchitectureTests` y
`ArchitectureRulesSelfTests` (una fixture negativa por regla nueva, SC-008).

```bash
./mvnw verify
```

Esperado: verde. Añade `StockPersistenceIT`, `OrderPersistenceIT`, `CatalogPersistenceIT`
ampliado, `InventoryApiIT`, `CartApiIT`, `OrderApiIT`, `OrderConcurrencyIT`, `ApplicationIT`
(9 paths) y, con P3, `OrderPlacedEventIT` y `OrderPlacedRetryIT`.

## 2. Arrancar contra una BD vacía

```bash
docker compose down -v
```

```bash
docker compose up -d postgres
```

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Esperado en el log: Flyway aplica hasta `5 - order schema` (o `6 - event publication registry`
con P3) y el servidor escucha en el **5271**.

```bash
docker compose exec postgres psql -U tractor -d tractor -c "select count(*) total, count(*) filter (where stock = 0) agotados from inventory_stock"
```

Esperado: `47 | 1` (FR-003).

## 3. P1: stock y sesión

```bash
curl -s localhost:5271/api/v1/inventory/CL-01-GR | jq
```
Esperado: `{ "sku": "CL-01-GR", "stock": 10 }`.

```bash
curl -s localhost:5271/api/v1/inventory/CL-01-GY | jq
```
Esperado: `stock: 0`.

```bash
curl -s -w '\n%{http_code}\n' localhost:5271/api/v1/inventory/XX-99-ZZ
```
Esperado: `404` con `VARIANT_NOT_FOUND` y `traceId`.

```bash
curl -s -w '\n%{http_code}\n' localhost:5271/api/v1/inventory/ABCDEFGHIJKLMNOPQRSTU
```
Esperado: `400` con `VALIDATION_ERROR` y `details.sku`.

```bash
curl -si localhost:5271/api/v1/cart
```
Esperado: `200`, cuerpo `{"items":[],"totalItems":0,"subTotal":0,"tax":0,"total":0}` (los ceros
pueden salir como `0.00`) y
`Set-Cookie: ts_session=<uuid>; Path=/; HttpOnly; SameSite=Lax`, sin `Secure` en `dev`.

```bash
curl -si -b 'ts_session=11111111-2222-4333-8444-555555555555' localhost:5271/api/v1/cart | grep -i set-cookie
```
Esperado: **sin salida**, porque una cookie válida se respeta.

```bash
curl -si -b 'ts_session=no-es-un-uuid' localhost:5271/api/v1/cart | grep -i set-cookie
```
Esperado: un `Set-Cookie` con un UUID **nuevo** (AS-1.7).

## 4. P2: pedido y recibo con curl

Se usa un *cookie jar* para que `POST` y `GET` compartan sesión:

```bash
curl -si -c /tmp/ts.jar -H 'Content-Type: application/json' -d '{"firstName":"Ada","lastName":"Lovelace","storeId":"store-a","extraPickups":"Big Micro Machines, Central Mall","items":[{"productId":"AU-07","variantId":"AU-07-YE","productName":"Verde Voyager","variantName":"AU-07-YE","price":4000,"quantity":1,"image":"x"}]}' localhost:5271/api/v1/orders
```
Esperado:
- `201` con `Location: /api/v1/orders/<id>` y `Set-Cookie: ts_session=…`.
- Recibo con `items[0].price = 5000.00` (no 4000, regresión .NET), `variantName = "AU-07-YE"`,
  `subTotal 5000.00`, `tax 1050.00`, `total 6050.00`,
  `extraPickups ["Big Micro Machines","Central Mall"]` y `status "PLACED"`.
- `AU-07-YE` pasa de 5 a 4 en `GET /inventory/AU-07-YE`.

Con el `<id>` del paso anterior:

```bash
curl -s -b /tmp/ts.jar localhost:5271/api/v1/orders/<id> | jq
```
Esperado: `200` y el **mismo** recibo, incluido `placedAt` (FR-020).

```bash
curl -s -w '\n%{http_code}\n' -b 'ts_session=11111111-2222-4333-8444-555555555555' localhost:5271/api/v1/orders/<id>
```
Esperado: `404 ORDER_NOT_FOUND` (otra sesión, SC-005). Lo mismo sin `-b` y con id `abc`.

Errores (cada uno debe dejar el stock igual):

| Cambio en el cuerpo del primer `POST` | Esperado |
|---|---|
| `"items":[]` | 409 `CART_EMPTY` |
| `"variantId":""` | 400 `VALIDATION_ERROR`, `details["items[0].variantId"]` |
| `"quantity":0` | 400 `VALIDATION_ERROR` |
| `"extraPickups":["a","b"]` | 400 `MALFORMED_REQUEST` |
| `"storeId":"store-z"` | 404 `STORE_NOT_FOUND` |
| `"variantId":"XX-99-ZZ"` | 404 `VARIANT_NOT_FOUND` |
| `"variantId":"CL-01-GY"` | 409 `OUT_OF_STOCK` |
| Dos líneas: `AU-07-YE` ×1 y `CL-01-GY` ×1 | 409 `OUT_OF_STOCK`; `AU-07-YE` **no** baja (rollback, AS-2.11) |

Comprobar que no quedan pedidos de los intentos fallidos:

```bash
docker compose exec postgres psql -U tractor -d tractor -c "select count(*) from order_order"
```
Esperado: solo los pedidos con 201.

### 4.1 Restaurar el stock sembrado (demo, review #2)

Los pedidos descuentan stock de verdad, y un solo `POST` de 50 líneas puede dejar la demo en 409.
Para volver a los valores de `V4__inventory_stock.sql` (los pedidos se conservan):

```bash
scripts/reset-demo-stock.sh
```
Usa `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` y `DB_PASSWORD` con los mismos valores por defecto que
`compose.yaml` (puerto `5432`; si publicaste Postgres en otro, p. ej. `DB_PORT=55432
scripts/reset-demo-stock.sh`). Solo acepta un host local. Si el equipo no tiene `psql`, usa el del
contenedor (`docker compose exec -T postgres psql`), que ignora `DB_HOST` y `DB_PORT`.
Esperado: `47 SKUs restored; CL-01-GR=10, CL-01-GY=0, AU-08-WH=1, AU-07-YE=5`, y
`curl -s localhost:5271/api/v1/inventory/CL-01-GR` vuelve a dar `"stock":10`.

## 5. P2 con el front real sin cambios (SC-001)

1. Backend en `dev` (puerto 5271) y front arrancado (MFEs en 4200–4203).
2. Abrir la ficha de **Heritage Workhorse** (`CL-01`): con la primera variante (`CL-01-GR`) se ve
   "10 in stock"; al cambiar a `CL-01-GY` se ve "Out of stock".
3. Añadir `AU-07-YE` al carrito y abrir el carrito. En DevTools → Application → Cookies de
   `localhost` aparece `ts_session` (HttpOnly). El carrito muestra 4000 (deuda aceptada, ADR 0011).
4. En el checkout: nombre, apellido, tienda y una recogida extra → confirmar.
5. Se llega a `/thanks?orderId=<uuid>` con las líneas, el subtotal 5000, el IVA 1050 y el total
   6050, y sin errores en la consola ni peticiones fallidas en la pestaña Network.
6. Recargar `/thanks`: el recibo se vuelve a pintar (la misma cookie).

### 5.1 Mitigación para la demo: SKUs con el mismo precio en carrito y recibo (review PR #5, A01)

Hasta la spec 005 `carrito-servidor` (2026-12-01) el carrito del front muestra el precio "desde" del
producto. En la demo, añade solo variantes cuyo precio coincide con ese "desde" (derivado de
`V3__catalog_dataset.sql`; stock de `V4`):

| Producto | SKU seguro (precio = "desde") | Evitar (precio mayor) |
|---|---|---|
| AU-02 | `AU-02-BL`, `AU-02-GG` (4000) | `AU-02-OG` |
| AU-03 | `AU-03-TQ` (1600) | `AU-03-PL`, `AU-03-RD`, `AU-03-YE` |
| AU-04 | `AU-04-BK` (8500) | `AU-04-RD` |
| AU-06 | `AU-06-MT` (2100) | `AU-06-CZ` |
| AU-07 | `AU-07-MT` (4000) | `AU-07-YE` |
| CL-01 | `CL-01-GR` (5700) | `CL-01-GY` (además, stock 0) |
| CL-08 | `CL-08-GR` (7750) | `CL-08-PI` |

Los otros 16 productos tienen todas sus variantes al mismo precio: cualquiera sirve (ojo con
`AU-08-WH`, que solo tiene 1 unidad). Ensayo recomendado: `CL-04-BL` ×1 + `AU-07-MT` ×1 → carrito
y recibo con subtotal 6200, IVA 1302 y total 7502. Antes de presentar, `scripts/reset-demo-stock.sh`.

## 6. P3: evento y log

> **Diferido** a una spec posterior (review 004): las fases 5–7 de `tasks.md` no se implementan en
> esta rama. Esta sección queda como guía para esa spec.

Tras confirmar un pedido (§4 o §5), el log de la app muestra (asíncrono, tras el commit):

```text
INFO [...] Confirmation email sent for order <id>
```

```bash
docker compose exec postgres psql -U tractor -d tractor -c "select event_type, listener_id, status, completion_date is not null as completed from event_publication order by publication_date desc limit 3"
```
Esperado: una fila por pedido con `event_type` `…order.api.OrderPlaced`, el listener de
`notifications` y `COMPLETED` / `t`. Un pedido fallido (§4, errores) no añade filas (AS-3.2).

El reintento (AS-3.3) lo cubre `OrderPlacedRetryIT`; no se prueba a mano.

## 7. Guardas de arquitectura (FR-025, FR-026, SC-008)

Prueba manual del ciclo: añadir temporalmente en `inventory` un
`@ApplicationModuleListener void on(com.tractorstore.order.api.OrderPlaced e) {}` y ejecutar:

```bash
./mvnw test -Dtest=ModularityTests
```
Esperado: **falla** por dependencia no permitida `inventory → order` (ciclo). Revertir el cambio.

## 8. Swagger (FR-031)

Abrir `http://localhost:5271/swagger-ui.html`: aparecen los tags `Inventory`, `Cart` y `Orders` con
4 operaciones documentadas (descripción, respuestas y errores), además de las 5 de `Catalog`.

## Troubleshooting

- **El `GET /orders/{id}` da 404 justo después del `POST` desde el navegador**: comprueba que la
  petición lleva la cookie (`withCredentials`) y que front y backend están en el mismo sitio
  (`localhost`). Con dominios distintos, `SameSite=Lax` no envía la cookie (research R4).
- **La app no arranca con `Schema-validation: wrong column type … event_publication`**: revisa que
  `V6` sea exactamente el DDL v2 de [research R11](research.md) y no el v1.
