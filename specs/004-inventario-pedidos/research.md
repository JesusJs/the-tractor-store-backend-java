# Research — 004 inventario y pedidos

Decisiones técnicas del plan. Cada una tiene el formato Decisión / Razón / Alternativas. "Verificado"
significa comprobado en esta sesión contra el código del repo o los jars de Maven Central, no de
memoria.

---

## R1. Migraciones y dataset de stock (FR-003, FR-004)

**Decisión**: tres migraciones nuevas, numeradas a partir de V4:

| Versión | Contenido | Prioridad |
|---|---|---|
| `V4__inventory_stock.sql` | Tabla `inventory_stock` + una fila por cada uno de los 47 SKUs de `V3` | P1 |
| `V5__order_schema.sql` | Tablas `order_order` y `order_line` | P2 |
| `V6__event_publication_registry.sql` | DDL v2 PostgreSQL de Modulith 2.1.1, literal (R11) | P3 |

Valores sembrados: **10 unidades** por SKU, con tres excepciones elegidas para la demo:

| SKU | Stock | Por qué |
|---|---|---|
| `CL-01-GY` | **0** | Es la segunda variante del primer producto (`CL-01`, Heritage Workhorse). En la ficha se pasa de "10 in stock" (`CL-01-GR`) a "Out of stock" con un clic. |
| `AU-08-WH` | **1** | "Última unidad" para enseñar el 409 en la demo con dos pestañas. |
| `AU-07-YE` | 5 | SKU de la regresión de precio (4000 en el carrito, 5000 en el recibo); con stock de sobra para repetir la demo. |

Esquema y datos van en la misma migración porque son 2 columnas y 47 filas. Separarlos como en
`V2`/`V3` solo añadiría un fichero.

**Razón**: FR-003 pide al menos un SKU a 0 y que las migraciones empiecen en V4. Que haya una fila
por SKU lo comprueba un IT que compara `inventory_stock` con `catalog_variant` (solo el test cruza
tablas; el código de producción no lo hace nunca).

**Alternativas**: stock aleatorio (no reproducible en los tests); dejar el SKU con stock 1 solo en
los tests (el test de concurrencia siembra sus propios valores igualmente, R15, pero el 409 en la
demo se ve mejor con datos reales).

## R2. Componente de sesión `ts_session` (FR-006, FR-007)

**Decisión**: `shared.infrastructure.web.SessionCookies` (`@Component`, público) con dos métodos:

- `Optional<UUID> read(HttpServletRequest)`: toma la **primera** cookie `ts_session`. Devuelve un
  valor solo si es un UUID canónico, que se comprueba con la regex
  `^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$` y después con `UUID.fromString`.
  `UUID.fromString` sola no basta porque acepta formas no canónicas como `1-1-1-1-1`.
- `UUID resolveOrIssue(HttpServletRequest, HttpServletResponse)`: si `read` devuelve un valor, lo
  respeta y **no** reenvía la cookie. Si no, genera `UUID.randomUUID()` y añade
  `Set-Cookie: ts_session=<uuid>; Path=/; HttpOnly; SameSite=Lax` (+ `Secure` si la propiedad está
  activa) con `org.springframework.http.ResponseCookie`, que soporta `SameSite`. No pone `Max-Age`:
  es una cookie de sesión del navegador (Assumption de la spec).

`secure` sale de `SessionCookieProperties` (`@ConfigurationProperties("tractorstore.session")`,
record `secureCookie`). Vale `false` en `application.yml` y `true` en `application-prod.yml`. Sigue
el mismo patrón que `CorsProperties`.

Uso: `CartController` y `OrderController#place` llaman a `resolveOrIssue` **antes** de ejecutar el
caso de uso, y `OrderController#get` solo llama a `read`, porque ese endpoint no crea cookie. La
cabecera `Set-Cookie` se añade a la respuesta antes de que el caso de uso pueda lanzar una
excepción, y `@ExceptionHandler` no reinicia las cabeceras. Por eso un `POST /orders` fallido
también deja la cookie, cosa que no hace daño.

**Razón**: es un único componente técnico en `shared`, como piden la ADR 0011 y la constitución II
v1.3.0. Es testeable sin contexto de Spring con `MockHttpServletRequest`/`Response`.

**Alternativas**:
- `HandlerMethodArgumentResolver` con una anotación `@SessionId`: más elegante, pero oculta que un
  `GET` puede escribir una cookie, y hacen falta dos variantes (crear o solo leer).
- `HttpSession` de Servlet: cookie `JSESSIONID` con estado en memoria; no cumple el contrato.
- Un filtro que ponga la cookie en todas las rutas: el contrato solo la crea en `GET /cart` y
  `POST /orders`.

## R3. Validez de la cookie y fijación de sesión

**Decisión**: un UUID canónico que envía el cliente se acepta aunque no lo haya generado el backend.

**Razón**: no hay autenticación ni datos de cuenta. La sesión solo liga pedidos, y elegir un UUID
propio solo da acceso a los pedidos hechos con ese mismo UUID. Adivinar el UUID de otra sesión
(122 bits aleatorios) no es viable. Firmar la cookie (HMAC) añadiría un secreto que gestionar
(principio VII). Se documenta como riesgo aceptado para `security-reviewer`.

## R4. `SameSite=Lax` y orígenes del front (riesgo de despliegue)

**Decisión**: se mantiene `SameSite=Lax`, como fijan el contrato y la constitución VI.

**Riesgo detectado**: `Lax` solo envía la cookie en peticiones XHR/fetch **same-site**.
- En local funciona: los MFEs en `localhost:4200-4203` y el backend en `localhost:5271` comparten
  sitio, porque el puerto no cuenta.
- En Azure, si el front y el backend quedan en dominios registrables distintos (p. ej.
  `*.azurestaticapps.net` y `*.azurecontainerapps.io`), el navegador **no** enviará `ts_session`.
  Cada `POST /orders` crearía una sesión nueva y `GET /orders/{id}` respondería 404.

Esto no bloquea esta spec, que se valida en local (quickstart §5). Habrá que resolverlo en la spec
de despliegue con el mismo sitio para ambos (dominio propio o un *reverse proxy* del front a
`/api`), o con una ADR que cambie a `SameSite=None; Secure`. Queda anotado para
`despliegue-azure-esqueleto`.

## R5. Jerarquía de errores: 409 (FR-017)

**Decisión**:
- Nueva clase abstracta `shared.error.ConflictException extends BusinessException` con
  `kind() = CONFLICT`.
- `GlobalExceptionHandler.handleBusiness` traduce `CONFLICT → 409` con el `code` de la excepción.
  Hoy cae en el 500, con un comentario que dice "spec 007 adds the 409".
- Excepciones nuevas por módulo, cada una en su `domain.exception`:

| Excepción | Módulo | Base | `code` |
|---|---|---|---|
| `StockNotFoundException` | inventory | `NotFoundException` | `VARIANT_NOT_FOUND` |
| `CartEmptyException` | order | `ConflictException` | `CART_EMPTY` |
| `StoreNotFoundException` | order | `NotFoundException` | `STORE_NOT_FOUND` |
| `VariantNotFoundException` | order | `NotFoundException` | `VARIANT_NOT_FOUND` |
| `OutOfStockException` | order | `ConflictException` | `OUT_OF_STOCK` |
| `OrderNotFoundException` | order | `NotFoundException` | `ORDER_NOT_FOUND` |

**Razón**: ninguna excepción cruza módulos. `order` no importa `catalog.domain.exception`, que es
interno, ni excepciones de `inventory`. Por eso `InventoryApi.decreaseStock` devuelve `boolean` y
`CatalogApi` devuelve lo que encuentra: el que decide el error es `order`. Así los tests de `order`
pueden mockear las APIs sin importar tipos internos de otros módulos. Que el `code`
`VARIANT_NOT_FOUND` se repita en tres módulos es aceptable: el código es un string del contrato,
no un tipo.

**Alternativas**: excepciones en `inventory.api` (se acopla `order` a tipos de excepción de otro
módulo y obliga a ampliar la regla de `api`); un `catalog.api.VariantNotFoundException` compartido
(lo mismo).

## R6. Validación de entrada y precedencia de errores (FR-017, FR-018)

**Decisión**: DTOs HTTP con Bean Validation y límites de la sección Assumptions:

```text
PlaceOrderRequest  firstName  @NotBlank @Size(max=100)
                   lastName   @NotBlank @Size(max=100)
                   storeId    @NotBlank @Size(max=20)
                   extraPickups          @Size(max=500)            (String, opcional)
                   items      @Size(max=50) List<@NotNull @Valid OrderItemRequest>   (sin @NotEmpty)
OrderItemRequest   variantId  @NotBlank @Size(max=20)
                   quantity   @NotNull @Min(1) @Max(99)  (Integer)
```

`@JsonIgnoreProperties(ignoreUnknown = true)` en los dos records: `price`, `productId`,
`productName`, `variantName` e `image` **no se declaran**, así que nunca se enlazan. Tampoco valen
para un 400: un `price` negativo o de otro tipo se ignora (Edge Cases). La anotación es explícita
para no depender del valor por defecto de `FAIL_ON_UNKNOWN_PROPERTIES` en Jackson 3/Boot 4.

Precedencia, de forma natural y sin código extra:
1. **400 `MALFORMED_REQUEST`**: Jackson no puede leer el cuerpo (`HttpMessageNotReadableException`).
   Incluye `extraPickups` como array: Jackson 3 no convierte un array en `String`
   (`UNWRAP_SINGLE_VALUE_ARRAYS` está desactivado), así que entra por aquí, como pide el Edge Case.
2. **400 `VALIDATION_ERROR`**: `@Valid` falla antes de entrar al controller. `details` usa la ruta
   del campo, p. ej. `items[0].variantId`.
3. **409 `CART_EMPTY`**: `items` es `null` o `[]`. Lo comprueba el caso de uso, no Bean Validation,
   porque el contrato pide 409 y no 400.
4. **404 `STORE_NOT_FOUND`** → 5. **404 `VARIANT_NOT_FOUND`** → 6. **409 `OUT_OF_STOCK`**: el orden
   de `PlaceOrderService` (R9).

`GET /inventory/{sku}`: `@Size(max = 20)` en el `@PathVariable` (400 por `HandlerMethodValidation`,
igual que en el catálogo).

`GET /orders/{id}`: **sin** `@Size`. Cualquier valor que no sea un UUID canónico → 404
`ORDER_NOT_FOUND`, que decide `OrderId.parse` sin tocar la BD. Así se cumple FR-018 (la entrada
abusiva no llega a la BD) sin un 400 que el contrato no prevé en esa ruta. Tomcat ya limita la
longitud de la URL.

## R7. `CatalogApi`: variantes por SKU y existencia de tienda (FR-010, FR-011)

**Decisión**: es la primera cara pública del catálogo.

```java
public interface CatalogApi {
  List<VariantView> findVariants(Collection<String> skus);   // las que existan; las desconocidas se omiten
  boolean storeExists(String storeId);                        // comparación exacta con catalog_store.id
}
public record VariantView(String sku, String productId, String productName,
                          String variantName, BigDecimal price, String image) {}
```

Se implementa en `catalog.application.service.CatalogApiAdapter` (package-private, `@Service`), que
delega en dos casos de uso nuevos (`FindVariantsUseCase`, `StoreExistsUseCase`). Esto sigue la
plantilla de la skill: "`XxxApiAdapter` implementa `api.XxxApi` delegando en casos de uso".

**Razón**: con una consulta para todas las líneas el pedido no hace N+1 (hasta 50 líneas). Devolver
una lista, en vez de lanzar una excepción, deja a `order` decidir el error (R5).

## R8. Consulta de variantes sin N+1 en el catálogo

**Decisión**: `VariantEntity` no tiene referencia a su producto (relación unidireccional
`ProductEntity.variants`, verificado). Por eso se consultan **productos** que contengan alguno de
los SKUs:

```java
@EntityGraph(attributePaths = {"category", "variants", "highlights"})
@Query("select p from ProductEntity p where exists "
     + "(select v from p.variants v where v.sku in :skus)")
List<ProductEntity> findAllContainingSkus(Collection<String> skus);
```

El puerto `ProductRepository.findByVariantSkus(Collection<String>)` devuelve `List<Product>` del
dominio. El caso de uso recorre `product.variants()` y construye una vista por cada SKU pedido que
exista. La subconsulta `exists` evita que el filtro y el *fetch join* compartan alias. Si
compartieran alias, Hibernate podría cargar solo las variantes filtradas. `CatalogPersistenceIT`
comprueba que se cargan **todas** las variantes del producto y que se ejecuta una sola sentencia
SQL (el mismo patrón que el FR-024 de 002).

`StoreRepository.existsById(String)` delega en el `existsById` de Spring Data.

**Alternativas**: añadir `@ManyToOne product` a `VariantEntity` (cambia un mapeo inmutable de 002
sin necesidad); una consulta por SKU (N+1).

**Transacción**: los servicios del catálogo son `@Transactional(readOnly = true)` con propagación
`REQUIRED`. Llamados desde `PlaceOrderService` se **unen** a la transacción de escritura del
pedido. Spring no valida la discrepancia de `readOnly` por defecto
(`validateExistingTransaction=false`), así que no cambia nada.

## R9. Caso de uso `PlaceOrderService` (FR-013 a FR-016)

**Decisión**: un único método `@Transactional` (propagación `REQUIRED`, aislamiento por defecto
`READ COMMITTED`):

```text
1. lines vacías o null                       → CartEmptyException (409)
2. !catalog.storeExists(storeId)             → StoreNotFoundException (404)
3. views = catalog.findVariants(skus distintos)
   primer SKU de las líneas sin vista        → VariantNotFoundException(sku) (404)
4. order = Order.place(OrderId.newId(), session, nombre…, lines con datos del catálogo, now(clock))
   (el dominio calcula subtotal, IVA y total)
5. para cada línea ORDENADA POR SKU (orden estable):
     !inventory.decreaseStock(sku, qty)      → OutOfStockException(sku) (409) ⇒ rollback de todo
6. orders.save(order)
7. [P3] events.orderPlaced(order)
8. return order
```

Puntos clave:
- **Atomicidad**: si falla el paso 5 o el 6, la `RuntimeException` atraviesa el proxy
  transaccional y se revierten todos los `UPDATE` ya hechos. No se ha guardado nada. Así se
  cumplen AS-11 y SC-004.
- **Orden por SKU en el paso 5**: dos pedidos de varias líneas que comparten SKUs bloquean las filas
  en el mismo orden, así que PostgreSQL no puede detectar un *deadlock* (`40P01`), que acabaría en
  un 500. Las líneas duplicadas del mismo SKU se procesan una a una, como dice la Clarification, y
  si entre las dos superan el stock falla la segunda.
- **Sin sobreventa**: el `UPDATE … WHERE stock >= :qty` de una segunda transacción concurrente
  espera el bloqueo de fila. Con `READ COMMITTED`, PostgreSQL vuelve a evaluar el `WHERE` sobre la
  versión confirmada y afecta 0 filas: 409 (AS-12, SC-003). `CHECK (stock >= 0)` en la tabla es una
  red de seguridad adicional.
- El descuento va **antes** de guardar el pedido para fallar pronto. En la misma transacción el
  orden no cambia el resultado.

**Alternativas**: leer el stock y luego escribir (prohibido por FR-015 y ADR 0011);
`SELECT … FOR UPDATE` + `UPDATE` (dos sentencias por línea, mismo efecto);
`@Version`/bloqueo optimista (reintentos innecesarios).

## R10. Dinero, IVA, `placedAt` y recibo idéntico (FR-013, FR-020)

**Decisión**:
- `Money`: record de dominio con `BigDecimal amount` normalizado a **escala 2** en el constructor
  (si hay que redondear, `HALF_UP`). No admite negativos. Ofrece `plus`, `times(int)` y
  `static ZERO`.
- `Vat`: `RATE = new BigDecimal("0.21")`; `Money taxOn(Money subTotal)` =
  `subTotal × 0.21 → setScale(2, HALF_UP)`. `@ParameterizedTest` con 0,05 → 0,01; 0,02 → 0,00
  (0,0042); 2,50 → 0,53 (0,525, mitad hacia arriba); 5000 → 1050,00; 43 065 000 → 9 043 650,00.
- **Tope**: 50 líneas × 99 × 8700 (variante más cara) = 43 065 000,00. Con IVA da
  52 108 650,00 < 99 999 999,99, el máximo de `NUMERIC(10,2)`. Los límites de R6 garantizan que
  ningún total desborda la columna.
- `placedAt = Instant.now(clock).truncatedTo(ChronoUnit.MILLIS)`. `Clock.systemUTC()` es un bean de
  `order.infrastructure.config`, así que el servicio solo depende de `java.time` (FR-030). Se trunca
  porque PostgreSQL guarda microsegundos y Linux da nanosegundos: sin truncar, el `placedAt` del
  `POST` y el del `GET` posterior **diferirían**, y FR-020 exige recibos idénticos.
  `OrderPersistenceIT` lo comprueba.
- JSON: `BigDecimal` se serializa como número (`5000.00`). El front lo formatea con
  `Intl.NumberFormat`.

## R11. Event Publication Registry: DDL exacto de Modulith 2.1.1 (FR-023) — verificado

**Verificado** en `spring-modulith-events-jdbc-2.1.1.jar` (Maven Central, publicado el 2026-08-25).
El jar trae dos versiones de esquema, `schemas/v1/` y `schemas/v2/`, y la **v2** es la del modelo
2.x (`status`, `completion_attempts`, `last_resubmission_date`). `V6` copia literalmente
`org/springframework/modulith/events/jdbc/schemas/v2/schema-postgresql.sql`:

```sql
CREATE TABLE IF NOT EXISTS event_publication
(
  id                     UUID NOT NULL,
  listener_id            TEXT NOT NULL,
  event_type             TEXT NOT NULL,
  serialized_event       TEXT NOT NULL,
  publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date        TIMESTAMP WITH TIME ZONE,
  status                 TEXT,
  completion_attempts    INT,
  last_resubmission_date TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash_idx ON event_publication USING hash(serialized_event);
CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx ON event_publication (completion_date);
```

Comprobaciones hechas sobre los jars 2.1.1:
- **Starter**: `spring-modulith-starter-jpa` 2.1.1 trae `events-api`, `events-core`,
  `events-jackson` y `events-jpa` (pom verificado). **No** trae `events-jdbc`: el DDL se toma del
  jar JDBC solo como fuente del SQL, no como dependencia.
- **Entidad JPA** (`events-jpa`, `javap`): `DefaultJpaEventPublication` →
  `@Table(name = "EVENT_PUBLICATION")`, con los campos `id` (UUID), `publicationDate`,
  `listenerId`, `serializedEvent`, `eventType` (`Class<?>` → texto), `completionDate`,
  `lastResubmissionDate`, `completionAttempts` (`int`) y `status` (`@Enumerated(STRING)`). Coincide
  columna a columna con el DDL v2. Con `ddl-auto=validate`, Hibernate valida esta entidad al
  arrancar. pgjdbc informa `TEXT` como `Types.VARCHAR`, que es compatible con `String`. Si algo no
  cuadra, la app no arranca y lo detecta cualquier IT (`ApplicationIT`).
- **Tabla de archivo**: `ArchivedJpaEventPublication` (`EVENT_PUBLICATION_ARCHIVE`) solo se registra
  con `spring.modulith.events.completion-mode=archive` (`@ConditionalOnProperty` verificado). Se
  usa el modo por defecto `update`, así que **no** se crea la tabla de archivo.
- **Inicialización automática**: la única propiedad es
  `spring.modulith.events.jdbc.schema-initialization.enabled` (por defecto `false`, del módulo
  JDBC). Con el starter JPA no hay inicialización de esquema, y Hibernate tampoco crea nada
  (`validate`). Cumple "sin inicialización automática" sin configurar nada. Aun así se declara
  `false` en `application.yml` para que quede explícito si alguien añade `events-jdbc`.

**Alternativas**: `spring-modulith-starter-jdbc` (el DDL coincide por construcción y no pasa por la
validación de Hibernate). Se descarta porque la skill `modular-architecture` y el backlog fijan
`starter-jpa`, y porque con `validate` la entidad sirve como comprobación extra del DDL.

## R12. `event_publication` sin prefijo de módulo

**Decisión**: excepción justificada en Complexity Tracking. Es la tabla del framework que exige el
principio V y su nombre está fijado en la entidad de Modulith. `TABLES_ARE_PREFIXED_WITH_MODULE`
solo analiza entidades de `com.tractorstore`, así que no hay que tocar la regla.

## R13. Entrega asíncrona, reintento y republicación (FR-024, US3)

**Verificado** (`events-core` 2.1.1): `EventPublicationAutoConfiguration$AsyncEnablingConfiguration`
aplica `@EnableAsync` si no hay otra configuración asíncrona
(`@ConditionalOnMissingBean(AbstractAsyncConfiguration)`). `@ApplicationModuleListener`
(= `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` + `@Transactional(REQUIRES_NEW)`) corre
por tanto en otro hilo **después del commit**. Un fallo del listener no deshace el pedido ni llega
a la respuesta HTTP.

Flujo del outbox:
1. Al publicar dentro de la transacción del pedido, Modulith inserta una fila en `event_publication`
   por listener, en la **misma** transacción. Si el pedido se revierte, la fila también (AS-2: no
   se publica nada).
2. Tras el commit corre el listener. Si termina bien, la fila queda `COMPLETED` con
   `completion_date`. Si falla, queda incompleta (`FAILED`).
3. Reintento:
   - **Producción**: `spring.modulith.events.republish-outstanding-events-on-restart: true`. Es
     suficiente con una sola réplica en ACA. Con varias réplicas habría entregas duplicadas, y se
     anota para la spec de despliegue.
   - **Test**: se inyecta `IncompleteEventPublications` y se llama a
     `resubmitIncompletePublications(p -> true)`. Después se comprueba con
     `CompletedEventPublications.findAll()` (API verificada en `events-api` 2.1.1).

Sin reintento programado (`@Scheduled`): no lo pide la spec (principio VII).

**Log**: el servicio de `notifications` escribe
`log.info("Confirmation email sent for order {}", orderId)`. El texto va en inglés porque la regla
del proyecto es código y logs en inglés. Equivale al "email enviado" de la spec, y los tests
buscan el `orderId` en la salida capturada.

## R14. Dónde viven publicador y listener (FR-022, FR-024, FR-030)

**Decisión**:
- Publicación: puerto `order.domain.port.out.OrderEventsGateway { void orderPlaced(Order order); }`.
  El adaptador `order.infrastructure.event.SpringOrderEventsGateway` traduce `Order` a
  `OrderPlaced` (record de `order.api`) y llama a `ApplicationEventPublisher.publishEvent`. El
  dominio no conoce el record de `api`.
- Consumo: `notifications.infrastructure.event.OrderPlacedListener` con
  `@ApplicationModuleListener void on(OrderPlaced event)` llama a
  `NotifyOrderPlacedUseCase.notify(UUID orderId)`. El caso de uso (application) solo registra el
  log.

**Razón**: FR-030 deja en `application` solo `@Service` y `@Transactional` de Spring. El sufijo
`Gateway` hace que la regla de FR-029 compruebe que el puerto vive en `domain.port.out`.

**Alternativas**: publicar desde `application` con `ApplicationEventPublisher` (rompe FR-030); que
el agregado registre eventos de dominio con `@DomainEvents` (Spring Data en el dominio, prohibido);
un puerto de salida de email en `notifications` (YAGNI: no hay email real, P3 recortable).

## R15. Tests con datos compartidos y concurrencia

**Decisión**:
- Los IT comparten contenedor y contexto, y la BD es la migrada. Cada IT que toca stock fija **sus**
  valores en `@BeforeEach` con `JdbcTemplate`
  (`update inventory_stock set stock = ? where sku = ?`) y no depende del orden de ejecución.
  Tocar tablas de otro módulo desde un test está permitido: las reglas de arquitectura importan
  `DoNotIncludeTests`.
- `OrderConcurrencyIT`: pone `AU-08-WH` a 1, lanza **dos** `POST /orders` en paralelo
  (`ExecutorService` + `CountDownLatch` para que salgan a la vez, cada uno con su cookie) y
  comprueba que los status son exactamente `{201, 409}`, que el stock final es 0 y que hay un solo
  pedido nuevo. Se repite con `@RepeatedTest(5)` para dar más oportunidades a la carrera.
- Rollback de varias líneas (AS-11): dos líneas con stock suficiente y una tercera sin stock →
  409, las tres filas de stock sin cambios y `count(order_order)` sin cambios.

## R16. Guardas de arquitectura (FR-028 a FR-030)

**Decisión**: reglas nuevas o ampliadas en `ArchitectureRules`, cada una construida desde una raíz
para poder probarla con fixtures en `archfixtures/`. Cada fixture lleva su caso negativo en
`ArchitectureRulesSelfTests`.

| FR | Regla | Fixture negativa |
|---|---|---|
| 028 | `TABLES_ARE_PREFIXED_WITH_MODULE` ampliada: un campo `@ElementCollection` sin `@CollectionTable` explícito, o un `@ManyToMany` del lado dueño (sin `mappedBy`) sin `@JoinTable`, **viola** la regla. También se comprueban `@SecondaryTable`/`@SecondaryTables`. | `manytomany` (sin `@JoinTable`), `secondarytable` (sin prefijo) y `collectionprefix` (ya existe) |
| 029 | `DOMAIN_IS_FRAMEWORK_FREE` + `jakarta..` (incluye `jakarta.validation`) y `io.swagger..` | `domainjakarta` (`@NotNull` en el dominio) |
| 029 | `DOMAIN_PORTS_LIVE_IN_PORT_OUT`: toda **interfaz** de `..domain..` y toda clase `*Repository`/`*Gateway` de `..domain..` vive en `..domain.port.out..`. Sustituye a `DOMAIN_REPOSITORIES_LIVE_IN_PORT_OUT`. | `domaingateway` (`ShopGateway` en `domain.port`) |
| 030 | `DOMAIN_DOES_NOT_DEPEND_ON_APPLICATION` | `domainapp` |
| 030 | `APPLICATION_USES_ONLY_SERVICE_AND_TRANSACTIONAL`: `..application..` solo depende de `org.springframework..` a través de `stereotype.Service` y `transaction.annotation.{Transactional, Propagation, Isolation}` | `appspring` (usa `ApplicationEventPublisher`) |
| 030 | `TRANSACTIONAL_ONLY_IN_APPLICATION`: ninguna clase ni método fuera de `..application..` lleva `@Transactional` **directo** (de Spring o `jakarta.transaction`) | `txoutsideapp` (controller con `@Transactional`) |

Notas:
- `@ApplicationModuleListener` está **meta**-anotado con `@Transactional`. La regla mira solo la
  anotación directa, así que el listener de `infrastructure.event` es válido: lo exige la
  constitución V y abre su propia transacción por diseño de Modulith.
- `@Modifying` en `JpaStockRepository` no necesita `@Transactional`: se ejecuta dentro de la
  transacción del caso de uso. Si se llamara sin transacción, Spring Data lanzaría
  `TransactionRequiredException`, que es lo que queremos.
- Los servicios del catálogo de 002 ya cumplen las tres reglas (solo `@Service` y
  `@Transactional`). Lo verifica el propio build al activar las reglas.
