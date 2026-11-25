# Contrato: caras públicas entre módulos (004)

Interfaces Java que un módulo expone a otro en su paquete `api` (`@NamedInterface("api")`). Solo
contienen records de intercambio y tipos del JDK: ni entidades, ni tipos de dominio, ni excepciones.
Dependencias permitidas (FR-026, ya declaradas en los `package-info.java`):

```text
order         → catalog :: api, inventory :: api, shared
cart          → shared
inventory     → shared
notifications → order :: api
```

## `catalog.api.CatalogApi`

```java
public interface CatalogApi {

  /**
   * Variants whose SKU is in {@code skus}, with the real variant price (not the "from" price).
   * Unknown SKUs are simply absent from the result; order of the result is unspecified.
   */
  List<VariantView> findVariants(Collection<String> skus);

  /** Whether {@code storeId} is exactly the id of a catalog store. */
  boolean storeExists(String storeId);
}

public record VariantView(
    String sku, String productId, String productName, String variantName,
    BigDecimal price, String image) {}
```
- Es de solo lectura y se une a la transacción del llamante.
- Ejecuta una consulta SQL por llamada, sea cual sea el número de SKUs (R8).

## `inventory.api.InventoryApi`

```java
public interface InventoryApi {

  /**
   * Atomically decreases the stock of {@code sku} by {@code quantity} only if enough units are
   * left (single conditional UPDATE). Runs in the caller's transaction, so a later failure there
   * rolls it back.
   *
   * @return true if decreased; false if not enough stock or the SKU has no stock row
   * @throws IllegalArgumentException if quantity < 1
   */
  boolean decreaseStock(String sku, int quantity);
}
```
- Necesita una transacción activa (la de `PlaceOrderService`).
- No lanza excepciones de negocio. Quien llama decide el error (`order` → `OUT_OF_STOCK`).

## `order.api.OrderPlaced` (evento, P3)

```java
public record OrderPlaced(UUID orderId, UUID sessionId, List<Line> lines) {
  public record Line(String sku, int quantity) {}
}
```
- Se publica una vez por pedido confirmado, dentro de su transacción. Se guarda en
  `event_publication` y se entrega después del commit.
- No se publica si la confirmación falla.
- Consumidor único: `notifications` (`@ApplicationModuleListener`). `inventory` **no** lo escucha:
  si lo hiciera, `ModularityTests` fallaría por el ciclo `order ↔ inventory`.
- El evento se serializa con Jackson en `serialized_event`. Cualquier campo nuevo debe ser
  compatible hacia atrás, porque puede haber publicaciones pendientes de reintento.
