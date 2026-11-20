# Research — 002 catálogo

Fase 0 del plan. Verificado el 2026-11-19 contra el front (`the-tractore-store`, commit `8bacec6`), el
repositorio oficial `neuland/tractor-store-blueprint` (rama `main`), el CDN
`blueprint.the-tractor.store` y los jars de Boot 4.1.2 / Modulith 2.1.1 / Hibernate 7.4.13 del
repositorio Maven local. Ningún `NEEDS CLARIFICATION` queda abierto; las decisiones que interpretan
la spec están marcadas con **(aprobada 2026-11-20)**.

## R1. Dataset oficial: fuente, tamaño y orden

- **Decision**: transcribir a `V3__catalog_dataset.sql` los ficheros
  `src/decide/database/database.json` (productos, highlights, variantes con color y precio) y
  `src/explore/database/database.json` (teasers, tiendas) del blueprint. Contenido verificado:
  **23 productos** (15 `CL-xx`, 8 `AU-xx`), **47 variantes** (SKUs únicos), **69 highlights**
  (3 por producto), **4 tiendas** (`store-a` … `store-d`), **2 teasers**.
- **Orden del dataset** (FR-002, FR-012): el de los listados de `explore` → `CL-01 … CL-15`,
  `AU-01 … AU-08`. Se guarda en una columna `position`. El orden de variantes y highlights es el de
  `decide`.
- **Consecuencia** (FR-012): la lista por defecto de recomendaciones es la primera variante de
  `CL-01`, `CL-02`, `CL-03`, `CL-04` → `CL-01-GR`, `CL-02-BL`, `CL-03-GR`, `CL-04-BL`.
- **Rationale**: `explore` es quien pinta la home y los listados en el blueprint; `decide` solo es
  dueño del detalle. Usar un único orden evita que listado y recomendaciones por defecto difieran.
- **Alternatives**: orden de `decide` (`AU` primero): el listado `all` empezaría por autónomos,
  distinto del blueprint.
- **Anomalías del dataset** (se corrigen al transcribir, quedan comentadas en la migración):
  - `AU-01` trae la clave `highlightsa` (errata del blueprint). Se cargan como sus highlights.
  - Los colores mezclan mayúsculas y minúsculas (`#C0C0C0`, `#dd5219`). Se guardan tal cual; el
    dominio los parsea sin distinguir mayúsculas.
  - Los colores de `decide` (`#hex`) y los de `explore.recommendations` (`rgb`) coinciden en las 47
    variantes (comprobado con un script).

## R2. Precios por variante (Assumption de la spec "a verificar en el plan")

- **Hallazgo**: **sí** hay productos con precios distintos entre variantes: `AU-02` (4000/4100),
  `AU-03` (1600–1900), `AU-04` (8500/8700), `AU-06` (2100/2200), `AU-07` (4000/5000), `CL-01`
  (5700/6200), `CL-08` (7750/7900). El resto tiene precio único.
- **Decision**: se aplica lo previsto en la spec: `price` del producto = menor precio de sus
  variantes ("desde", FR-003). Se documenta como **limitación conocida** en la ADR del contrato: el
  carrito (spec 007) usará el precio real de la variante, que puede ser mayor que el mostrado en el
  listado/detalle.
- Los precios del dataset son enteros; se guardan como `NUMERIC(10,2)` (`1000.00`). Jackson 3
  serializa `BigDecimal` como número (`1000.00`); el front lo trata como `number`.

## R3. URLs de imagen (FR-009)

- **Comprobado en el front**: todas las imágenes de catálogo se pintan con `[src]="x.image"` tal
  cual (`home`, `stores`, detalle en `mfe-decide`, recomendaciones, `product-card`). Los `.webp` de
  `apps/shell/src/assets` y `packages/mfe-explore/src/assets` (`AU-07-YE.webp`, `AU-08-WH.webp`)
  **no se sirven**: en los `project.json` solo se copian los assets de `public/`, que solo contiene
  `favicon.ico` e imágenes de maqueta (`tractor-1.png`…). No hay 47 imágenes en el front, así que una
  ruta relativa daría 404. El footer del shell ya carga `https://blueprint.the-tractor.store/cdn/...`.
- **Decision**: el backend devuelve **URLs absolutas del CDN oficial**, ya resueltas en la
  migración: `https://blueprint.the-tractor.store/cdn/img/product/400/<SKU>.webp`,
  `.../cdn/img/store/400/store-N.webp` y `.../cdn/img/scene/500/<classics|autonomous>.webp`
  (el blueprint guarda el patrón con `[size]`; tamaños disponibles: product 200/400/800, store
  200/400/800, scene 500/1000).
- **Verificación**: las **53 URLs** (47 variantes + 4 tiendas + 2 escenas) responden `200
  image/webp`, con `access-control-allow-origin: *`.
- **Alternatives**: (a) guardar el patrón `[size]` y resolver la URL con una propiedad
  `catalog.image-base-url`: más flexible, pero añade configuración y obliga a resolver también en
  `catalog.api` para el carrito; YAGNI hasta que haya otro CDN. Si cambia, se hace con una migración
  nueva (`UPDATE`). (b) Copiar las imágenes al front: cambia el front, prohibido.
- **Riesgo** (se documenta en la ADR 0008, aprobado 2026-11-20): dependencia de un CDN de
  terceros en la demo; si cae o cambia sus rutas, el catálogo se ve sin imágenes. Mitigación: el
  fallback del `product-card` (`placehold.co`) evita huecos en los listados, `README` (spec 010)
  menciona el CDN, y cambiar de origen es una migración `UPDATE` nueva.

## R4. `description` derivada y highlights con punto final **(aprobada 2026-11-20)**

- **Hallazgo**: los highlights de `AU-01` y `AU-02` terminan en `.`; los de los otros 21 productos
  no. Aplicando FR-007 literalmente (`join(". ")`) saldría `"...coverage.. Hybrid..."`.
- **Decision**: `description` = highlights en orden, **quitando un `.` final de cada uno**, unidos
  con `". "`. El ejemplo de FR-007 (`["A","B"]` → `"A. B"`) se cumple igual, y `["A.","B"]` →
  `"A. B"`. Los highlights se guardan y se devuelven tal cual (fidelidad al dataset).
  `ProductTest` incluye un caso con los highlights reales de `AU-01` (terminan en `.`) que espera
  `"Precision GPS mapping optimizes field coverage. Hybrid engine ensures eco-friendly extended
  operation. Fully autonomous with smart obstacle detection and terrain adaptation"`.
- **Alternatives**: normalizar el texto en la migración (altera el dataset oficial que el detalle
  muestra en `highlights`); aplicar la regla literal (texto con `..` en la demo).

## R5. Modelo de persistencia y lectura sin N+1 (FR-024)

- **Decision**: entidades JPA de solo lectura (`@org.hibernate.annotations.Immutable`) en
  `catalog.infrastructure.persistence`:
  - `ProductEntity` → `@ManyToOne(fetch = LAZY) CategoryEntity category`;
    `@ElementCollection @CollectionTable(name = "catalog_product_highlight") @OrderColumn(name =
    "position") List<String> highlights`; `@OneToMany @JoinColumn(name = "product_id")
    @OrderColumn(name = "position") List<VariantEntity> variants` (unidireccional: la variante no
    conoce al producto).
  - Consultas en `JpaProductRepository` con `@EntityGraph(attributePaths = {"category",
    "variants", "highlights"})`: `findAllByOrderByPositionAsc()`,
    `findAllByCategoryCodeOrderByPositionAsc(String)`, `findWithDetailsById(String)`.
- **Rationale**: con `@OrderColumn` las dos colecciones son listas indexadas, no *bags*, así que
  Hibernate puede traerlas en la misma consulta sin `MultipleBagFetchException` y conserva el orden
  del dataset. El producto cartesiano es pequeño (≤ 23 × 4 × 3 = 276 filas para `all`). Resultado:
  **una sola sentencia SQL** por petición de catálogo (detalle, listado o recomendaciones).
- **Verificación**: el IT del adaptador activa `hibernate.generate_statistics` y comprueba
  `getPrepareStatementCount() == 1` para el detalle y para el listado.
- **Alternatives**: `JOIN FETCH` en JPQL (equivalente, más verboso); `@BatchSize` (≥ 2 consultas);
  `Set` en vez de `List` (pierde el orden del dataset).
- **Tipos y `ddl-auto=validate`**: Hibernate valida el tipo JDBC de cada columna. Por eso `color`
  es `VARCHAR(7)` (no `CHAR(7)`, que PostgreSQL reporta como `bpchar` y falla la validación) y las
  posiciones son `INTEGER` (no `SMALLINT`, que no casa con `int`). Precio `NUMERIC(10,2)` ↔
  `@Column(precision = 10, scale = 2) BigDecimal`.

## R6. Carga del catálogo y recomendaciones en memoria

- **Decision**: el caso de uso de recomendaciones carga el catálogo completo (`findAll`, 1 consulta)
  y delega en una política de dominio pura (`ColorRecommendationPolicy`) que calcula promedio,
  distancias, exclusiones, deduplicado por producto, desempate por SKU y lista por defecto.
- **Rationale**: 47 variantes; calcular en Java es trivial, determinista y 100 % testeable con un
  test unitario parametrizado (constitución III, skill `api-contract`). La distancia en SQL
  acoplaría la regla de negocio a PostgreSQL.
- **Alternatives**: SQL con `sqrt(power(...))` (lógica de negocio en la BD); caché (fuera de
  alcance, constitución VII).
- **Reglas** (FR-010 – FR-013): SKUs recibidos → `trim`, se descartan vacíos, duplicados cuentan
  una vez, desconocidos se ignoran. Si no queda ninguno → lista por defecto (R1). Si no: color
  promedio (media aritmética de R, G y B, en `double`), distancia euclidiana de cada variante no
  recibida, orden por `(distancia, sku)`, primera aparición de cada producto, límite 4. Productos
  cuyas variantes están todas entre las recibidas desaparecen solos (no tienen variantes candidatas).
- **Forma de respuesta**: igual que un ítem de listado, con `variants` reordenado para que el SKU
  recomendado vaya **primero** y el resto conserve el orden del dataset (el front usa
  `variants[0]`). `image` y `price` son los del producto (FR-006 remite a FR-003).

## R7. Módulo compartido para errores y web transversal **(aprobada 2026-11-20, ADR 0009)**

- **Decision**: nuevo paquete `com.tractorstore.shared` declarado como módulo Modulith abierto:
  `@ApplicationModule(displayName = "Shared", type = ApplicationModule.Type.OPEN,
  allowedDependencies = {})` (API verificada en `spring-modulith-api-2.1.1.jar`: `Type.OPEN`
  expone todos sus tipos). Contenido:
  - `shared.error` (Java puro, usable desde `domain`): `BusinessException` (abstracta, `code`,
    `ErrorKind` = `NOT_FOUND | CONFLICT`), `NotFoundException`.
  - `shared.infrastructure.web`: `ApiError` (record), `GlobalExceptionHandler`
    (`@RestControllerAdvice`), `TraceIdFilter`, `CorsConfiguration` + `CorsProperties`.
  - `catalog` pasa a `allowedDependencies = {"shared"}`; los demás módulos lo añadirán en su spec.
- **Rationale**: FR-019 pide una jerarquía común y un único punto de traducción a HTTP reutilizable
  por inventario, carrito y pedidos. Si viviera en `catalog`, los demás dependerían de `catalog`
  solo por los errores. `ErrorKind` evita que el dominio conozca códigos HTTP.
- **Constitución**: el principio II enumera cinco módulos; añadir `shared` es un cambio de
  arquitectura → **ADR 0009** y enmienda de la constitución a **v1.1.0** (principio II: cinco
  módulos de dominio + `shared` técnico). Ambas hechas el 2026-11-20, antes del código.
- **Alternatives**: (a) paquete raíz `com.tractorstore.error` fuera de módulos: Modulith trata
  cada paquete directo bajo la raíz como módulo, así que es lo mismo sin nombre explícito.
  (b) Duplicar el handler por módulo: viola FR-019.

## R8. Formato de error y excepciones de Spring (FR-014 – FR-018)

- **Decision**: `GlobalExceptionHandler` traduce:

  | Excepción | HTTP | `code` | `details` |
  |---|---|---|---|
  | `BusinessException` (`ErrorKind.NOT_FOUND`) | 404 | el de la excepción (`PRODUCT_NOT_FOUND`, `CATEGORY_NOT_FOUND`, `VARIANT_NOT_FOUND`) | — |
  | `MethodArgumentNotValidException` (`@Valid @RequestBody`) | 400 | `VALIDATION_ERROR` | campo → mensaje |
  | `HandlerMethodValidationException` (`@PathVariable`/`@RequestParam` con constraints, Spring 6.1+) | 400 | `VALIDATION_ERROR` | parámetro → mensaje |
  | `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException` | 400 | `MALFORMED_REQUEST` | — |
  | `NoResourceFoundException` (ruta sin handler, Spring 6.1+) | 404 | `NOT_FOUND` | — |
  | `HttpRequestMethodNotSupportedException` | 405 | `METHOD_NOT_ALLOWED` | — |
  | `Exception` (resto, incl. `DataAccessException`) | 500 | `INTERNAL_ERROR` | — (stacktrace solo al log, nivel `ERROR`) |

  `ApiError` lleva `@JsonInclude(NON_NULL)` para omitir `details` (anotación en
  `com.fasterxml.jackson.annotation`, que Jackson 3 conserva; skill `spring-boot-4-notes` §4).
- **Códigos genéricos nuevos** `NOT_FOUND` y `METHOD_NOT_ALLOWED` (edge case de la spec): se añaden
  al contrato en la ADR de FR-025.
- **Validación en catálogo**: `@PathVariable @Size(max = 20)` en `id` y `filter` (un valor más
  largo → 400 `VALIDATION_ERROR`; un valor corto pero desconocido → 404 de negocio, como pide la
  spec). `skus` **no** lleva constraints: FR-013 exige que recomendaciones no falle nunca; un SKU
  raro simplemente no coincide. El caso `MethodArgumentNotValidException` (cuerpo) se prueba con un
  controller de test en el `@WebMvcTest` del handler, porque catálogo no tiene cuerpos.
- **Alternatives**: `ProblemDetail` (RFC 9457) de Spring: forma distinta del contrato.

## R9. `traceId`: filtro, MDC y logs (FR-015)

- **Decision**: `TraceIdFilter extends OncePerRequestFilter`, `@Order(Ordered.HIGHEST_PRECEDENCE)`:
  1. Lee `X-Trace-Id`; lo acepta solo si cumple `^[A-Za-z0-9._-]{1,64}$` (evita inyección en logs
     y cabeceras); si no, genera `UUID.randomUUID()` sin guiones (32 hex).
  2. `MDC.put("traceId", …)`, `response.setHeader("X-Trace-Id", …)` **antes** de `chain.doFilter`
     (así va también en respuestas con éxito), y `MDC.remove` en `finally`.
  3. El handler lee el valor del MDC para `ApiError.traceId`.
- **Logs**: `logging.pattern.level: "%5p [%X{traceId:-}]"` en `application.yml` → cada línea de log
  de la petición lleva el id. No se usa Micrometer Tracing (OpenTelemetry fuera de alcance).
- **Rationale**: un filtro cubre también errores que no pasan por un controller (404 de ruta).
- **Alternatives**: `HandlerInterceptor` (no ve errores previos al handler); Micrometer Tracing
  (dependencia y formato W3C que el contrato no pide).

## R10. CORS y puerto (FR-027)

- **Decision**: `CorsProperties` (`@ConfigurationProperties("tractorstore.cors")`, `List<String>
  allowedOrigins`) enlazada a `${CORS_ALLOWED_ORIGINS:}` en `application.yml` (vacío → ningún
  origen permitido). En `application-dev.yml`: default
  `http://localhost:4200,http://localhost:4201,http://localhost:4202,http://localhost:4203` y
  `server.port: 5271`. `CorsConfiguration implements WebMvcConfigurer`:
  `addMapping("/api/**")`, `allowedOrigins(...)`, `allowCredentials(true)`, métodos
  `GET, POST, DELETE, OPTIONS`, cabeceras `Content-Type, X-Trace-Id`, `exposedHeaders("X-Trace-Id",
  "Location")`, `maxAge(3600)`.
- **Fail-fast**: si algún origen es `*` o contiene `*`, el arranque falla (`IllegalStateException`
  en la validación de las propiedades). Constitución VI.
- **Origen no permitido**: Spring (`DefaultCorsProcessor`) responde **403** con cuerpo `Invalid
  CORS request` y sin `Access-Control-Allow-Origin`. Ese cuerpo **no** usa el formato único.
  **(aprobada 2026-11-20)** Se acepta y se documenta en la ADR 0008: el navegador nunca expone esa
  respuesta al front, y personalizar el `CorsProcessor` es complejidad sin valor para la demo.
- **Alternatives**: `@CrossOrigin` por controller (repetido, y no lee entorno); `CorsFilter`
  propio (duplica lo que da Spring).

## R11. Deuda de arquitectura de la revisión 001 (FR-028)

- **#4 `allowEmptyShould`**: se elimina de las 4 reglas existentes, que ya tendrán clases de
  `catalog`. Además, `ArchitectureRulesSelfTests` importa con `ClassFileImporter` un paquete de
  *fixtures* de test (`com.tractorstore.archfixtures`, excluido del análisis principal por
  `DoNotIncludeTests`) con una violación por regla y comprueba que `rule.check(...)` lanza
  `AssertionError`. Así la prueba negativa entra en el build. Las reglas se exponen como constantes
  en una clase `ArchitectureRules` reutilizada por ambos tests.
- **#7 `api` no expone internos**: `noClasses().that().resideInAPackage("..api..")
  .should().dependOnClassesThat().resideInAnyPackage("..domain..", "..application..",
  "..infrastructure..")`. Se mantiene `allowEmptyShould(true)` **solo** en esta regla porque
  `catalog.api` sigue vacío en esta spec (lo llenará la 007 con lo que necesite el carrito); la
  self-test garantiza que no pasa en vacío por estar mal escrita.
- **`shared.error` sin frameworks** (ADR 0009): la regla `domainIsFrameworkFree` pasa a cubrir
  `..domain..` y `com.tractorstore.shared.error..`, porque los dominios dependen de ella.
- **`@Entity` solo en persistence**: `classes().that().areAnnotatedWith(Entity.class).should()
  .resideInAPackage("com.tractorstore.*.infrastructure.persistence..")`.
- **Tablas con prefijo del módulo**: `ArchCondition` propia: toda `@Entity` de
  `com.tractorstore.<m>...` debe llevar `@Table(name = "<m>_…")`, y todo campo con
  `@CollectionTable`/`@JoinTable` también. Cubre la regresión .NET "entidades duplicadas".
- **Web nunca devuelve `@Entity`** (constitución I): queda cubierto por `webDoesNotUsePersistence`
  + `@Entity` solo en persistence.
- **#10 `ddl-auto`**: `JpaSchemaValidationTests` (unitario, surefire) carga cada
  `application*.yml` con `YamlPropertySourceLoader` y exige que el base declare `validate` y que
  ningún perfil lo sobrescriba con otro valor ni active `spring.jpa.generate-ddl`. Más
  `CatalogPersistenceIT` comprueba en el `Environment` del perfil `test` que vale `validate`.
- **No incluido** (fuera de FR-028): review 001 #15 (`domain → application`, Spring permitido en
  `application`, `@Transactional` solo en `application`). Anotado en la fila 008 de
  `docs/backlog.md` (decisión del 2026-11-20).

## R12. Swagger (FR-026) e IT existente

- **Decision**: cada endpoint lleva `@Operation(summary, description)` y `@ApiResponse` por status
  (200 y los errores posibles) con `@Content(schema = @Schema(implementation = ApiError.class))`.
  `@Tag(name = "Catalog")` en el controller.
- **Impacto en 001**: `ApplicationIT.openApiDocumentHasNoPaths` y `businessRoutesDoNotExistYet`
  dejan de ser ciertos. Se sustituyen por: el documento contiene exactamente los 5 paths de catálogo;
  `GET /api/v1/catalog/products` (sin id) → 404 `NOT_FOUND` con el formato único.

## R13. Coordenadas de test en Boot 4.1.2 (skill `spring-boot-4-notes`)

Verificadas en los jars locales:
- `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` / `AutoConfigureMockMvc`.
- `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`.
- `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase` (`replace = NONE`
  para usar el contenedor de `TestcontainersConfiguration`).
- `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`).
- `NoResourceFoundException` en `spring-webmvc` 7.0.10.
No hacen falta dependencias nuevas en `pom.xml`.
