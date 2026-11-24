---

description: "Task list for 002 catálogo"
---

# Tasks: Catálogo (persistencia, errores y endpoints de catálogo)

**Input**: Design documents from `specs/002-catalogo/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/catalog-api.md, contracts/errors.md, quickstart.md

**Tests**: SÍ. La constitución (principio III, NO NEGOCIABLE) exige prueba primero y de adentro
hacia afuera. Cada test se escribe y se ve **fallar (🔴)** antes de la tarea de implementación que lo
pone en verde (🟢). Regresiones .NET con `@DisplayName("regresión: …")`.

**Organization**: por historia de usuario. Orden de fases por dependencia real: US5 (datos) es la
base de US1/US2/US3; US4 (errores completos, P2) va antes que US3 (P2) porque el caso 400 de `skus`
necesita la traducción de la validación que completa US4.

**Revisión 2026-11-20**: correcciones aprobadas de `/speckit-analyze` (C1, C2, M1–M5, L1–L5).

**Refactor 2026-11-20** (tras T058, sin cambio de comportamiento): rutas actualizadas a la estructura
hexagonal explícita (`domain/{model,service,exception,port/out}`, `application/{port/in,service,model}`,
`infrastructure/persistence/{entity,repository,adapter}`, `infrastructure/web/{controller,dto,mapper}`);
las interfaces de caso de uso pasan a llamarse `*UseCase`. Ver skill `hexagonal-architecture`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede hacer en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: historia a la que pertenece (US1…US5)
- Rutas relativas a la raíz del repo. Paquete raíz `com.tractorstore`:
  `src/main/java/com/tractorstore/…` y `src/test/java/com/tractorstore/…`
- Antes de escribir configuración, tests o serialización: cargar la skill `spring-boot-4-notes`
  (coordenadas de Boot 4.1.2 en research R13: `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`,
  `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`,
  `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`, `@MockitoBean`,
  `RestTestClient`, Jackson 3). **Sin dependencias nuevas en `pom.xml`.**

---

## Phase 1: Setup (contrato y módulos)

**Purpose**: contrato primero (FR-025) y declarar el módulo técnico `shared` (ADR 0009, constitución v1.1.0, ya aprobadas).

- [X] T001 Crear `docs/adr/0008-contrato-catalogo-segun-front.md` a partir de `docs/adr/0000-plantilla.md` (estado: aprobada 2026-11-19): la forma de catálogo la manda el front (tabla "Contexto" de `specs/002-catalogo/spec.md`); códigos genéricos nuevos `NOT_FOUND` (404, ruta sin handler) y `METHOD_NOT_ALLOWED` (405); `skus` validado con `@Size(max = 50)`/`@Size(max = 20)` → 400 `VALIDATION_ERROR` solo ante entrada abusiva; limitaciones conocidas: precio "desde" = mínimo de variantes (research R2, p. ej. `AU-07-YE` cuesta 5000 y se muestra 4000), rechazo CORS 403 `Invalid CORS request` fuera del formato único (R10), dependencia del CDN `https://blueprint.the-tractor.store/cdn/img/...` (R3)
- [X] T002 Reescribir la sección Catalog de `docs/api-contract.md` con los 5 endpoints y tipos de `specs/002-catalogo/contracts/catalog-api.md` (incluida la validación de `skus`), añadir `NOT_FOUND` y `METHOD_NOT_ALLOWED` a la tabla de errores según `specs/002-catalogo/contracts/errors.md`, retirar las marcas ⚠️ resueltas y enlazar la ADR 0008 (depende de T001)
- [X] T003 [P] Crear el módulo abierto `src/main/java/com/tractorstore/shared/package-info.java` con `@ApplicationModule(displayName = "Shared", type = ApplicationModule.Type.OPEN, allowedDependencies = {})` y javadoc "technical module: error hierarchy, trace id, CORS (ADR 0009)"
- [X] T004 [P] Cambiar `src/main/java/com/tractorstore/catalog/package-info.java` a `allowedDependencies = {"shared"}`
- [X] T005 Actualizar `src/test/java/com/tractorstore/ModularityTests.java`: renombrar `detectsExactlyTheFiveBusinessModules` → `detectsTheFiveBusinessModulesPlusShared` y esperar `"catalog", "inventory", "cart", "order", "notifications", "shared"`; ejecutar `./mvnw test -Dtest=ModularityTests` en verde (depende de T003, T004)

---

## Phase 2: Foundational (bloquea todas las historias)

**Purpose**: dominio puro de `catalog` y de `shared.error`, puertos de salida y la parte mínima de la traducción de errores y `traceId` que usan todos los endpoints.

**⚠️ CRITICAL**: ninguna historia empieza hasta terminar esta fase.

### Tests (🔴 primero)

- [X] T006 [P] Escribir `src/test/java/com/tractorstore/catalog/domain/model/ColorTest.java` (JUnit 5 + AssertJ, `@ParameterizedTest`): `fromHex` acepta `#C0C0C0` y `#dd5219` sin distinguir mayúsculas; rechaza con `IllegalArgumentException` `null`, `"C0C0C0"`, `"#C0C0C"`, `"#GGGGGG"`; canales "0 ≤ valor ≤ 255" (fuera de rango → excepción); `distanceTo` euclidiana `√(Δr² + Δg² + Δb²)` (negro↔blanco = `441.67…`, con `within(1e-9)`; simétrica; consigo mismo = 0); `average` media por canal en `double` (p. ej. `#000000`+`#FFFFFF` → `127.5`), colección vacía → excepción
- [X] T007 [P] Escribir `src/test/java/com/tractorstore/catalog/domain/model/ProductTest.java`: `startPrice()` = mínimo de precios (`CL-01`: 5700.00/6200.00 → `5700.00`); `mainImage()` = imagen de `variants[0]`; `skus()` en orden; `brand()` = `"Classic"`/`"Autonomous"`; `description()` con `["A","B"]` → `"A. B"`, `["A.","B"]` → `"A. B"` y el caso real de `AU-01` (research R4) que espera `"Precision GPS mapping optimizes field coverage. Hybrid engine ensures eco-friendly extended operation. Fully autonomous with smart obstacle detection and terrain adaptation"`; `highlights()` se devuelven tal cual (con su `.`); `skusStartingWith("CL-01-GY")` → `["CL-01-GY","CL-01-GR"]`; invariantes: `id`/`name` no vacíos, `variants` ≥ 1, listas inmutables (modificar la lista de origen no afecta); `Variant` rechaza `sku`/`name` vacíos y `price` ≤ 0 (precio "> 0, escala 2"); `Category` rechaza campos vacíos
- [X] T008 [P] Escribir `src/test/java/com/tractorstore/catalog/domain/model/CategoryFilterTest.java`: `of("all")` → `isAll()` verdadero y sin código; `of("classic")` y `of("autonomous")` → no `all`, conservan el código exacto; `of("Classic")`, `of("classics")` y `of("ALL")` → no son `all` y conservan el valor sin normalizar (comparación exacta, sensible a mayúsculas); `null` o vacío → `IllegalArgumentException`
- [X] T009 [P] Escribir `src/test/java/com/tractorstore/catalog/domain/exception/CatalogExceptionsTest.java`: `ProductNotFoundException("CL-99")` → `code() == "PRODUCT_NOT_FOUND"`, `getMessage() == "Product CL-99 not found"`, `kind() == ErrorKind.NOT_FOUND`, es `NotFoundException` y `BusinessException`; `CategoryNotFoundException("classics")` → `CATEGORY_NOT_FOUND`, `Category classics not found`; `VariantNotFoundException("AU-05-ZZ")` → `VARIANT_NOT_FOUND`, `Variant AU-05-ZZ not found`
- [X] T010 [P] Escribir `src/test/java/com/tractorstore/shared/infrastructure/web/TraceIdFilterTest.java` (unitario con `MockHttpServletRequest`/`MockHttpServletResponse`/`MockFilterChain`): sin cabecera genera 32 hex y lo pone en `X-Trace-Id` de la respuesta **antes** de la cadena; con `X-Trace-Id: demo-trace-1` lo reutiliza; inválidos (vacío, 65 caracteres, `"a b"`, `"x\ny"`) se ignoran y se genera uno nuevo (regex `^[A-Za-z0-9._-]{1,64}$`); el MDC `traceId` está puesto durante la cadena y se limpia después, incluso si la cadena lanza
- [X] T011 [P] Escribir la primera versión de `src/test/java/com/tractorstore/shared/infrastructure/web/GlobalExceptionHandlerTest.java` (`@WebMvcTest` con un `@RestController` de prueba anidado en el test y una subclase de prueba de `NotFoundException` con `code` `TEST_NOT_FOUND`, `@Import({GlobalExceptionHandler.class, TraceIdFilter.class})`): `BusinessException` NOT_FOUND → 404 con su `code` y mensaje, sin clave `details`; `RuntimeException("SELECT secret FROM x")` → 500 `INTERNAL_ERROR`, `message == "Unexpected error"`, el cuerpo no contiene `Exception`, `SELECT`, `at com.` ni nombres de clase; en ambos `Content-Type: application/json`, `$.traceId` == cabecera `X-Trace-Id`, y con petición `X-Trace-Id: demo-trace-1` ambos valen `demo-trace-1`. T044 (US4) amplía este mismo archivo

### Implementación (🟢)

- [X] T012 [P] Crear `src/main/java/com/tractorstore/shared/error/ErrorKind.java` (enum `NOT_FOUND`, `CONFLICT`; javadoc: `CONFLICT` lo usarán carrito y pedidos), `src/main/java/com/tractorstore/shared/error/BusinessException.java` (abstracta, extiende `RuntimeException`, `String code()`, `abstract ErrorKind kind()`, constructor `(String code, String message)`) y `src/main/java/com/tractorstore/shared/error/NotFoundException.java` (abstracta, `kind() = NOT_FOUND`). Java puro: cero imports de Spring/Jackson
- [X] T013 [P] Crear `src/main/java/com/tractorstore/catalog/domain/model/Color.java`: `record Color(double red, double green, double blue)` con validación "0 ≤ valor ≤ 255", `static fromHex(String)` (`#RRGGBB`, sin distinguir mayúsculas), `double distanceTo(Color)`, `static average(Collection<Color>)` (vacía → `IllegalArgumentException`) — T006 en verde
- [X] T014 [P] Crear `src/main/java/com/tractorstore/catalog/domain/model/Variant.java` (record `sku`, `name`, `Color color`, `BigDecimal price`, `String image`; `sku` y `name` no vacíos, `price` > 0) y `src/main/java/com/tractorstore/catalog/domain/model/Category.java` (record `code`, `brandLabel`, `teaserTitle`, `teaserImage`; todos no vacíos)
- [X] T015 [P] Crear `src/main/java/com/tractorstore/catalog/domain/model/Store.java` (record `id`, `name`, `address`, `city`, `image`) y `src/main/java/com/tractorstore/catalog/domain/model/Recommendation.java` (`record Recommendation(Product product, Variant variant)`)
- [X] T016 Crear `src/main/java/com/tractorstore/catalog/domain/model/Product.java` (clase final inmutable: `id`, `name`, `Category category`, `List<String> highlights`, `List<Variant> variants` con `List.copyOf`; derivados `startPrice()`, `mainImage()`, `skus()`, `brand()`, `description()` quitando un `.` final por highlight y uniendo con `". "`, `skusStartingWith(String sku)`) — T007 en verde (depende de T013, T014)
- [X] T017 [P] Crear `src/main/java/com/tractorstore/catalog/domain/model/CategoryFilter.java`: `static CategoryFilter of(String raw)`; `"all"` → `ALL` (sin categoría, `isAll()`), cualquier otro valor se conserva como código a resolver; comparación **exacta** sensible a mayúsculas (`Classic`, `classics` no son `all` ni se normalizan) — T008 en verde
- [X] T018 Crear las excepciones de dominio que extienden `NotFoundException`: `src/main/java/com/tractorstore/catalog/domain/exception/ProductNotFoundException.java` (`PRODUCT_NOT_FOUND`, mensaje `Product <id> not found`), `CategoryNotFoundException.java` (`CATEGORY_NOT_FOUND`, `Category <filter> not found`) y `VariantNotFoundException.java` (`VARIANT_NOT_FOUND`, `Variant <sku> not found`; javadoc: reservada para el carrito, sin uso en 002) — T009 en verde (depende de T012)
- [X] T019 Crear los puertos de salida en `src/main/java/com/tractorstore/catalog/domain/port/out/`: `ProductRepository.java` (`List<Product> findAll()` orden `position`; `List<Product> findByCategory(String code)` orden `position`; `Optional<Product> findById(String id)`), `CategoryRepository.java` (`List<Category> findAll()` orden `position`; `Optional<Category> findByCode(String code)`), `StoreRepository.java` (`List<Store> findAll()` orden `position`) (depende de T014–T016)
- [X] T020 Crear `src/main/java/com/tractorstore/shared/infrastructure/web/TraceIdFilter.java` (`OncePerRequestFilter`, `@Component`, `@Order(Ordered.HIGHEST_PRECEDENCE)`, constantes `HEADER = "X-Trace-Id"` y `MDC_KEY = "traceId"`, `MDC.remove` en `finally`) — T010 en verde; y añadir a `src/main/resources/application.yml` `logging.pattern.level: "%5p [%X{traceId:-}]"` (FR-015)
- [X] T021 Crear `src/main/java/com/tractorstore/shared/infrastructure/web/ApiError.java` (`record ApiError(String code, String message, Map<String, String> details, String traceId)` con `@JsonInclude(JsonInclude.Include.NON_NULL)` de `com.fasterxml.jackson.annotation`) y la primera versión de `src/main/java/com/tractorstore/shared/infrastructure/web/GlobalExceptionHandler.java` (`@RestControllerAdvice`): `BusinessException` con `ErrorKind.NOT_FOUND` → 404 con el `code` y mensaje de la excepción; `Exception` → 500 `INTERNAL_ERROR`, mensaje `Unexpected error`, stacktrace solo al log en `ERROR`; `traceId` leído del MDC. **Sin** traducción de `ErrorKind.CONFLICT` (la añade la spec 007; hasta entonces cae en el 500 genérico, research R8) — T011 en verde (depende de T012, T020)

**Checkpoint**: `./mvnw test` verde con `ColorTest`, `ProductTest`, `CategoryFilterTest`, `CatalogExceptionsTest`, `TraceIdFilterTest`, `GlobalExceptionHandlerTest`, `ModularityTests`, `HexagonalArchitectureTests`.

---

## Phase 3: User Story 5 - Catálogo reproducible en cualquier entorno (Priority: P1) 🎯 base del MVP

**Goal**: esquema `catalog_*` y dataset oficial (23 productos, 47 variantes, 69 highlights, 4 tiendas, 2 categorías) creados por Flyway al arrancar, validados con `ddl-auto=validate`, y adaptadores JPA que leen el catálogo en una sola sentencia SQL.

**Independent Test**: `./mvnw verify -Dit.test=CatalogPersistenceIT` contra BD vacía (Testcontainers): recuentos 23/47/69/4/2, orden del dataset, 1 sentencia por lectura, segunda migración sin cambios. A mano: quickstart §2.

### Tests (🔴 primero)

- [X] T022 [P] [US5] Escribir `src/test/java/com/tractorstore/JpaSchemaValidationTests.java` (unitario, surefire): carga cada `src/main/resources/application*.yml` con `YamlPropertySourceLoader`; exige que el base declare `spring.jpa.hibernate.ddl-auto=validate` y que ningún perfil lo sobrescriba con otro valor ni active `spring.jpa.generate-ddl` (FR-021, FR-028, review 001 #10)
- [X] T023 [P] [US5] Escribir `src/test/java/com/tractorstore/catalog/infrastructure/persistence/adapter/CatalogPersistenceIT.java` (`@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `@Import({TestcontainersConfiguration.class, ProductRepositoryAdapter.class, CategoryRepositoryAdapter.class, StoreRepositoryAdapter.class})`, perfil `test`, `spring.jpa.properties.hibernate.generate_statistics=true`): recuentos con `JdbcTemplate` 23 productos / 47 variantes / 69 highlights / 4 tiendas / 2 categorías; `findAll()` empieza por `CL-01` y acaba en `AU-08`; `findByCategory("classic")` → 15 (`CL-01`…`CL-15`), `"autonomous"` → 8; `findById("AU-01")` trae sus 3 highlights tal cual (con `.`, anomalía `highlightsa` corregida) y variantes en orden; `findById("AU-07")` → variantes `AU-07-MT`, `AU-07-YE`, `startPrice` `4000.00`; `findById("CL-99")` vacío; tiendas `store-a`…`store-d` con `address` `Astronaut Way 1`…; todas las imágenes empiezan por `https://blueprint.the-tractor.store/cdn/img/`; `Statistics.getPrepareStatementCount() == 1` para detalle y para listado `all` (FR-024); volver a ejecutar `Flyway.migrate()` aplica 0 migraciones y los recuentos no cambian (US5-2); el `Environment` resuelve `spring.jpa.hibernate.ddl-auto=validate`; `@DisplayName("regresión: el catálogo sale de migraciones reales, no de datos en memoria")`

### Implementación (🟢)

- [X] T024 [US5] Crear `src/main/resources/db/migration/V2__catalog_schema.sql` exactamente según data-model.md §4: `catalog_category` (`code VARCHAR(20) PK`, `brand_label VARCHAR(50) NOT NULL`, `teaser_title VARCHAR(100) NOT NULL`, `teaser_image VARCHAR(255) NOT NULL`, `position INTEGER NOT NULL UNIQUE`); `catalog_product` (`id VARCHAR(20) PK`, `name VARCHAR(100) NOT NULL`, `category_code VARCHAR(20) NOT NULL FK → catalog_category(code)`, `position INTEGER NOT NULL UNIQUE`, índice en `category_code`); `catalog_product_highlight` (`product_id VARCHAR(20) NOT NULL FK → catalog_product(id)`, `position INTEGER NOT NULL CHECK (position >= 0)`, `text VARCHAR(255) NOT NULL`, `PK (product_id, position)`); `catalog_variant` (`sku VARCHAR(20) PK`, `product_id VARCHAR(20) NOT NULL FK`, `position INTEGER NOT NULL CHECK (position >= 0)`, `name VARCHAR(100) NOT NULL`, `color VARCHAR(7) NOT NULL CHECK (color ~ '^#[0-9A-Fa-f]{6}$')`, `price NUMERIC(10,2) NOT NULL CHECK (price > 0)`, `image VARCHAR(255) NOT NULL`, `UNIQUE (product_id, position)`); `catalog_store` (`id VARCHAR(20) PK`, `name VARCHAR(100) NOT NULL`, `address VARCHAR(150) NOT NULL`, `city VARCHAR(100) NOT NULL`, `image VARCHAR(255) NOT NULL`, `position INTEGER NOT NULL UNIQUE`). Nada de `CHAR`/`SMALLINT` (research R5). No tocar `V1__baseline.sql`
- [X] T025 [US5] Crear `src/main/resources/db/migration/V3__catalog_dataset.sql` transcribiendo del blueprint oficial (`neuland/tractor-store-blueprint`, `src/decide/database/database.json` y `src/explore/database/database.json`, research R1): 2 categorías (`classic`/`Classic`/`Classic Tractors`/`https://blueprint.the-tractor.store/cdn/img/scene/500/classics.webp`, position 0; `autonomous`/`Autonomous`/`Autonomous Tractors`/`.../scene/500/autonomous.webp`, position 1); 23 productos con `position` `CL-01`=0 … `CL-15`=14, `AU-01`=15 … `AU-08`=22; 69 highlights (position 0..2, texto tal cual; comentario SQL sobre la errata `highlightsa` de `AU-01`); 47 variantes en orden de `decide` (position desde 0 por producto), color tal cual (mayúsculas/minúsculas mezcladas), precio con 2 decimales, `image = https://blueprint.the-tractor.store/cdn/img/product/400/<SKU>.webp`; 4 tiendas `store-a`…`store-d` (`street` → `address`, `image = .../cdn/img/store/400/store-N.webp`). Comprobar que coinciden los valores citados en `contracts/catalog-api.md` (CL-01, AU-07, tiendas)
- [X] T026 [P] [US5] Crear `src/main/java/com/tractorstore/catalog/infrastructure/persistence/entity/CategoryEntity.java` y `StoreEntity.java`: `@Entity @Immutable @Table(name = "catalog_category" | "catalog_store")`, constructor `protected` sin argumentos, solo getters; campos según data-model.md §5 (`brandLabel`→`brand_label`, `teaserTitle`, `teaserImage`, `position`)
- [X] T027 [P] [US5] Crear `src/main/java/com/tractorstore/catalog/infrastructure/persistence/entity/VariantEntity.java`: `@Entity @Immutable @Table(name = "catalog_variant")`, `@Id sku`, `name`, `color`, `@Column(precision = 10, scale = 2) BigDecimal price`, `image`; sin referencia al producto
- [X] T028 [US5] Crear `src/main/java/com/tractorstore/catalog/infrastructure/persistence/entity/ProductEntity.java`: `@Entity @Immutable @Table(name = "catalog_product")`, `@Id id`, `name`, `position`, `@ManyToOne(fetch = LAZY) @JoinColumn(name = "category_code") CategoryEntity category`, `@ElementCollection @CollectionTable(name = "catalog_product_highlight", joinColumns = @JoinColumn(name = "product_id")) @OrderColumn(name = "position") @Column(name = "text") List<String> highlights`, `@OneToMany @JoinColumn(name = "product_id") @OrderColumn(name = "position") List<VariantEntity> variants` (depende de T026, T027)
- [X] T029 [P] [US5] Crear los repositorios Spring Data en `src/main/java/com/tractorstore/catalog/infrastructure/persistence/repository/`: `JpaProductRepository.java` (`findAllByOrderByPositionAsc()`, `findAllByCategoryCodeOrderByPositionAsc(String)`, `findWithDetailsById(String)`, los tres con `@EntityGraph(attributePaths = {"category", "variants", "highlights"})`), `JpaCategoryRepository.java` (`findAllByOrderByPositionAsc()`), `JpaStoreRepository.java` (`findAllByOrderByPositionAsc()`); públicos para que los use `adapter/` (refactor de paquetes) (depende de T028)
- [X] T030 [US5] Crear los adaptadores `@Component` `ProductRepositoryAdapter.java`, `CategoryRepositoryAdapter.java` y `StoreRepositoryAdapter.java` en `src/main/java/com/tractorstore/catalog/infrastructure/persistence/adapter/`, que implementan los puertos de `catalog.domain.port.out` y mapean entidad → dominio (color con `Color.fromHex`; las entidades nunca salen de `infrastructure.persistence`) — T022 y T023 en verde (depende de T019, T024, T025, T029)

**Checkpoint**: `./mvnw verify` verde; quickstart §2 da `23 | 47 | 69 | 4`.

---

## Phase 4: User Story 1 - Explorar el catálogo: home, listados y tiendas (Priority: P1) 🎯 MVP

**Goal**: `GET /api/v1/catalog/home`, `/categories/{filter}` y `/stores` con la forma exacta del front, en el puerto 5271 con CORS para 4200–4203.

**Independent Test**: `CatalogControllerTest` (`@Nested` Home/Categories/Stores) + `CorsIT` + quickstart §3 (home, listados, tiendas), §5 y §6 con el front (home, Classics/Autonomous/All, tiendas).

### Tests (🔴 primero)

- [X] T031 [P] [US1] Escribir `src/test/java/com/tractorstore/catalog/application/service/GetHomeTeasersServiceTest.java`, `GetCategoryListingServiceTest.java` y `ListStoresServiceTest.java` (Mockito sobre `CategoryRepository`, `ProductRepository`, `StoreRepository`): home devuelve las categorías en orden; listado `all` usa `findAll()` y devuelve `CategoryListing("all", …)`; `classic` comprueba `findByCode("classic")` y usa `findByCategory("classic")`; `Classic`, `classics` y `tractors` → `CategoryNotFoundException` con `code` `CATEGORY_NOT_FOUND`; tiendas en orden
- [X] T032 [P] [US1] Escribir `src/test/java/com/tractorstore/catalog/infrastructure/web/controller/CatalogControllerTest.java` (`@WebMvcTest(CatalogController.class)` + `@Import({GlobalExceptionHandler.class, TraceIdFilter.class})` + `@MockitoBean` de los casos de uso) con `@Nested Home` (array de 2, `$[0].id/title/image/filter`, `filter` `classic`/`autonomous`), `@Nested Categories` (`$.category`, `$.availableFilters == ["all","classic","autonomous"]`, `$.products[0]` con exactamente `id, name, brand, price, image, variants, description` — sin `stock`, `enginePower` ni colores (FR-008) —, `price` numérico `5700.00`, `variants` array de strings; caso de uso lanza `CategoryNotFoundException` → 404 `$.code == "CATEGORY_NOT_FOUND"`, sin `details`, `$.traceId` igual a la cabecera `X-Trace-Id`) y `@Nested Stores` (array con `id, name, address, city, image`, sin `street`); todas las respuestas llevan `X-Trace-Id`
- [X] T033 [P] [US1] Escribir `src/test/java/com/tractorstore/shared/infrastructure/web/CorsIT.java` (`@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@ActiveProfiles({"test", "dev"})` para probar los orígenes por defecto **reales** de `application-dev.yml`; `@Import(TestcontainersConfiguration.class)` (la `@ServiceConnection` sustituye la BD de dev); `RestTestClient`): `Origin` `http://localhost:4200`…`4203` (simple y preflight `OPTIONS` con `Access-Control-Request-Method: GET`) → `Access-Control-Allow-Origin: <origin>`, `Access-Control-Allow-Credentials: true`, `Access-Control-Expose-Headers` incluye `X-Trace-Id`; `Origin: http://evil.example` → 403 sin `Access-Control-Allow-Origin`; sin `Origin` → sin cabeceras CORS; y un test unitario de `CorsProperties` que falla al construir con `*` o `http://*.example` (fail-fast)

### Implementación (🟢)

- [X] T034 [P] [US1] Crear en `src/main/java/com/tractorstore/catalog/application/` los puertos de entrada (`port/in/`) y servicios (`service/`, `@Service` + `@Transactional(readOnly = true)`): `GetHomeTeasersUseCase.java` + `GetHomeTeasersService.java` (`List<Category> get()`), `model/CategoryListing.java` (`record CategoryListing(String filter, List<Product> products)`), `GetCategoryListingUseCase.java` + `GetCategoryListingService.java` (`CategoryListing get(String filter)` usando `CategoryFilter.of`; desconocido → `CategoryNotFoundException`), `ListStoresUseCase.java` + `ListStoresService.java` (`List<Store> list()`) — T031 en verde
- [X] T035 [P] [US1] Crear `src/main/java/com/tractorstore/catalog/infrastructure/web/controller/CatalogPaths.java` (`BASE = "/api/v1/catalog"`) y en `src/main/java/com/tractorstore/catalog/infrastructure/web/dto/` los records `TeaserResponse(id, title, image, filter)`, `ProductItemResponse(id, name, brand, BigDecimal price, image, List<String> variants, description)`, `CategoryResponse(category, List<ProductItemResponse> products, List<String> availableFilters)`, `StoreResponse(id, name, address, city, image)` y `../mapper/CatalogDtoMapper.java` con el mapeo de data-model.md §6 (`TeaserResponse.id = filter = code`; `price = startPrice()`; `image = mainImage()`; `variants = skus()`; `availableFilters = ["all","classic","autonomous"]`). Ningún DTO con `stock`, `enginePower` ni color
- [X] T036 [US1] Crear `src/main/java/com/tractorstore/catalog/infrastructure/web/controller/CatalogController.java` (`@RestController`, `@RequestMapping(CatalogPaths.BASE)`, `@Tag(name = "Catalog")`; **sin** `@Validated` de clase: activaría la validación AOP (`ConstraintViolationException`) en lugar de la nativa de MVC (`HandlerMethodValidationException`) que traduce R8) con `GET /home`, `GET /categories/{filter}` (`@PathVariable @Size(max = 20) String filter`) y `GET /stores`; solo llama a casos de uso; cada método con `@Operation(summary, description)` y `@ApiResponse` 200 y errores posibles (404/400/500) con `@Content(schema = @Schema(implementation = ApiError.class))` — T032 en verde (depende de T034, T035)
- [X] T037 [US1] Crear `src/main/java/com/tractorstore/shared/infrastructure/web/CorsProperties.java` (`@ConfigurationProperties("tractorstore.cors")`, `List<String> allowedOrigins`, `@Validated`/constructor que lanza `IllegalStateException` si algún origen es o contiene `*`) y `src/main/java/com/tractorstore/shared/infrastructure/web/CorsConfiguration.java` (`WebMvcConfigurer`: `addMapping("/api/**")`, `allowedOrigins(...)`, `allowCredentials(true)`, métodos `GET, POST, DELETE, OPTIONS`, cabeceras `Content-Type, X-Trace-Id`, `exposedHeaders("X-Trace-Id", "Location")`, `maxAge(3600)`; con lista vacía no permite ningún origen)
- [X] T038 [US1] Configurar CORS y puerto: en `src/main/resources/application.yml` `tractorstore.cors.allowed-origins: ${CORS_ALLOWED_ORIGINS:}`; en `src/main/resources/application-dev.yml` `server.port: 5271` y `tractorstore.cors.allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:4200,http://localhost:4201,http://localhost:4202,http://localhost:4203}` (FR-027; nunca comodín) — T033 en verde (depende de T037)
- [X] T039 [US1] Ajustar `src/test/java/com/tractorstore/ApplicationIT.java`: eliminar `openApiDocumentHasNoPaths` y `businessRoutesDoNotExistYet` (dejan de ser ciertos con el controller; se sustituyen en T056); `./mvnw verify` verde

**Checkpoint**: quickstart §3 (home, listados 23/15/8, tiendas 4) y §5 (CORS) pasan; el front pinta home, listados y tiendas.

---

## Phase 5: User Story 2 - Ver el detalle de un producto (Priority: P1)

**Goal**: `GET /api/v1/catalog/products/{id}` con `{ id, name, brand, price, image, description, variants, highlights }`.

**Independent Test**: `CatalogControllerTest.ProductDetail` + `GetProductDetailServiceTest` + `curl -s localhost:5271/api/v1/catalog/products/AU-07 | jq` igual al ejemplo de `contracts/catalog-api.md`; `CL-99` → 404 `PRODUCT_NOT_FOUND`.

### Tests (🔴 primero)

- [X] T040 [P] [US2] Escribir `src/test/java/com/tractorstore/catalog/application/service/GetProductDetailServiceTest.java` (Mockito sobre `ProductRepository`): id existente devuelve el `Product`; `CL-99` → `ProductNotFoundException` con `code` `PRODUCT_NOT_FOUND` y mensaje `Product CL-99 not found`
- [X] T041 [US2] Añadir `@Nested ProductDetail` a `src/test/java/com/tractorstore/catalog/infrastructure/web/controller/CatalogControllerTest.java`: 200 con exactamente `id, name, brand, price, image, description, variants, highlights` (sin `stock`/`enginePower`), `variants == ["AU-07-MT","AU-07-YE"]`, `price == 4000.00`, `highlights` tal cual; caso de uso lanza `ProductNotFoundException` → 404 `PRODUCT_NOT_FOUND` sin `details` y con `traceId` = cabecera

### Implementación (🟢)

- [X] T042 [P] [US2] Crear `src/main/java/com/tractorstore/catalog/application/port/in/GetProductDetailUseCase.java` + `GetProductDetailService.java` (`Product get(String id)`; vacío → `ProductNotFoundException`) — T040 en verde
- [X] T043 [US2] Crear el record `src/main/java/com/tractorstore/catalog/infrastructure/web/dto/ProductDetailResponse.java` (`id, name, brand, BigDecimal price, image, description, List<String> variants, List<String> highlights`) y su método en `CatalogDtoMapper.java`; añadir `GET /products/{id}` (`@PathVariable @Size(max = 20) String id`) con `@Operation`/`@ApiResponse` (200, 400, 404, 500) a `src/main/java/com/tractorstore/catalog/infrastructure/web/controller/CatalogController.java` — T041 en verde (depende de T042)

**Checkpoint**: quickstart §3 (detalle `AU-07`) y §4 (`CL-99` → 404) pasan; el front pinta el detalle.

---

## Phase 6: User Story 4 - Errores con forma única y trazables (Priority: P2)

**Goal**: toda respuesta de error con `{ code, message, details?, traceId }` y `X-Trace-Id`; 400 `VALIDATION_ERROR`/`MALFORMED_REQUEST`, 404 `NOT_FOUND`, 405 `METHOD_NOT_ALLOWED`, 500 `INTERNAL_ERROR` sin internos, y el `traceId` en cada línea de log.

**Independent Test**: `GlobalExceptionHandlerTest` + quickstart §4 (404, 400 con `details.id`, 405, `X-Trace-Id: demo-trace-1` reutilizado y visible en el log).

### Tests (🔴 primero)

- [X] T044 [US4] Ampliar `src/test/java/com/tractorstore/shared/infrastructure/web/GlobalExceptionHandlerTest.java` (creado en T011) con: `@Valid @RequestBody` inválido → 400 `VALIDATION_ERROR` con `details` campo → mensaje; `@PathVariable @Size(max = 20)` excedido y `@RequestParam List<@Size(max = 20) String>` con un elemento excedido → 400 `VALIDATION_ERROR` con `details.<param>`; JSON ilegible, tipo de parámetro incorrecto y parámetro obligatorio ausente → 400 `MALFORMED_REQUEST` sin `details`; ruta inexistente → 404 `NOT_FOUND`; `POST` a un `GET` → 405 `METHOD_NOT_ALLOWED`; `DataAccessException` → 500 `INTERNAL_ERROR` sin internos; en todos `$.traceId` == cabecera `X-Trace-Id` y `details` ausente (no `null`) cuando no aplica. Caso de log (FR-015, M1): con `@ExtendWith(OutputCaptureExtension.class)`, una petición con `X-Trace-Id: demo-trace-1` que acaba en 500 deja en `CapturedOutput` la línea de log `ERROR` con `[demo-trace-1]`
- [X] T045 [P] [US4] Añadir a `CatalogControllerTest.java` los casos de validación del catálogo: `/categories/<21 caracteres>` → 400 `VALIDATION_ERROR` con `details.filter`; `/products/<30 X>` → 400 con `details.id`

### Implementación (🟢)

- [X] T046 [US4] Completar `src/main/java/com/tractorstore/shared/infrastructure/web/GlobalExceptionHandler.java` con la tabla de research R8: `MethodArgumentNotValidException` y `HandlerMethodValidationException` → 400 `VALIDATION_ERROR` con `details` (campo/parámetro → mensaje); `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException` → 400 `MALFORMED_REQUEST`; `NoResourceFoundException` → 404 `NOT_FOUND`; `HttpRequestMethodNotSupportedException` → 405 `METHOD_NOT_ALLOWED`; resto → 500 `INTERNAL_ERROR` (log `ERROR` con stacktrace). Sigue sin traducir `ErrorKind.CONFLICT` (spec 007) — T044 y T045 en verde

**Checkpoint**: quickstart §4 pasa completo, incluido `[demo-trace-1]` en el log.

---

## Phase 7: User Story 3 - Recomendaciones por cercanía de color (Priority: P2)

**Goal**: `GET /api/v1/catalog/recommendations?skus=a,b` → hasta 4 productos por distancia RGB al color promedio, `variants[0]` = SKU recomendado, lista por defecto si no hay SKUs conocidos, nunca errores de negocio; `skus` validado (`@Size(max = 50)` en la lista, `@Size(max = 20)` por elemento).

**Independent Test**: `ColorRecommendationPolicyTest` con los casos de referencia de `contracts/catalog-api.md` + `curl "localhost:5271/api/v1/catalog/recommendations?skus=AU-01-SI" | jq '[.[] | .variants[0]]'` → `["AU-07-MT","CL-15-SD","CL-08-GR","CL-11-SK"]`.

### Tests (🔴 primero)

- [X] T047 [P] [US3] Escribir `src/test/java/com/tractorstore/catalog/domain/service/ColorRecommendationPolicyTest.java` (catálogo de prueba construido en el test con colores conocidos + `@ParameterizedTest`): orden ascendente por distancia al color promedio; empate de distancia → orden por SKU; excluye los SKUs recibidos; un producto como mucho una vez con su variante más cercana; producto con todas sus variantes recibidas no aparece; `LIMIT = 4`; SKU repetido cuenta una vez; `trim` y vacíos (`"AU-01-SI, ,"`) ignorados; desconocidos ignorados; `null`, `[]`, `[""]` y solo desconocidos → lista por defecto `(product, variants[0])` de los 4 primeros productos; nunca lanza. Regresiones: `@DisplayName("regresión: recomienda por color y nunca por categoría")` (un SKU clásico recibe un autónomo más cercano) y `@DisplayName("regresión: sin skus responde la lista por defecto, nunca error")`
- [X] T048 [P] [US3] Escribir `src/test/java/com/tractorstore/catalog/application/service/GetRecommendationsServiceTest.java` (Mockito sobre `ProductRepository.findAll()`): delega en la política con el catálogo completo; con `null` devuelve la lista por defecto; una sola llamada a `findAll()`
- [X] T049 [US3] Añadir `@Nested Recommendations` a `src/test/java/com/tractorstore/catalog/infrastructure/web/controller/CatalogControllerTest.java`: `?skus=AU-01-SI,CL-01-GR` llega al caso de uso como lista de 2; sin `skus`, `skus=` y `skus=,,` → 200; 50 SKUs de 20 caracteres → 200 (límite incluido); cada ítem con la forma de `ProductItemResponse` y `variants[0]` igual al SKU recomendado seguido del resto en orden del dataset. Caso de validación (C2, constitución VI): 51 SKUs → 400 `VALIDATION_ERROR` con `details.skus` y un SKU de 21 caracteres → 400 `VALIDATION_ERROR` con `details.skus`; en ambos el caso de uso no se invoca

### Implementación (🟢)

- [X] T050 [US3] Crear `src/main/java/com/tractorstore/catalog/domain/service/ColorRecommendationPolicy.java` (Java puro, `LIMIT = 4`, `List<Recommendation> recommend(List<Product> catalog, Collection<String> requestedSkus)`) con las reglas 1–4 de data-model.md §1 / research R6 — T047 en verde
- [X] T051 [US3] Crear `src/main/java/com/tractorstore/catalog/application/port/in/GetRecommendationsUseCase.java` + `GetRecommendationsService.java` (`List<Recommendation> get(List<String> skus)`, `skus` puede ser `null`; instancia o recibe la política como objeto de dominio sin anotaciones Spring en ella) — T048 en verde (depende de T050)
- [X] T052 [US3] Añadir a `CatalogDtoMapper.java` el mapeo `Recommendation` → `ProductItemResponse` con `variants = product.skusStartingWith(variant.sku())` y a `CatalogController.java` `GET /recommendations` con `@RequestParam(required = false) @Size(max = 50) List<@Size(max = 20) String> skus` (FR-013, research R8) y `@Operation`/`@ApiResponse` (200, 400, 500) — T049 en verde (depende de T046, T051)
- [X] T053 [US3] Escribir `src/test/java/com/tractorstore/catalog/GetRecommendationsDatasetIT.java` (`@SpringBootTest(webEnvironment = NONE)` + `@Import(TestcontainersConfiguration.class)`, inyecta `GetRecommendationsUseCase`; dataset real) con los casos de referencia de `contracts/catalog-api.md`: `AU-01-SI` → `AU-07-MT, CL-15-SD, CL-08-GR, CL-11-SK`; `CL-01-GR` → `CL-09-GR, CL-14-GR, CL-07-GR, CL-04-RD`; `AU-02-OG,AU-02-BL,AU-02-GG` → `AU-04-BK, CL-09-GR, CL-01-GR, AU-06-MT`; `CL-02-BL,AU-05-ZZ` → `CL-13-BL, AU-03-TQ, CL-09-BL, CL-04-TQ`; vacío/`NOPE-00` → `CL-01-GR, CL-02-BL, CL-03-GR, CL-04-BL` (SC-003, SC-004)

**Checkpoint**: quickstart §3 (recomendaciones, incluido el 400 por SKU de 21 caracteres) pasa; el front pinta recomendaciones en detalle y home.

---

## Phase 8: Polish & Cross-Cutting (guardas de arquitectura, E2E, documentación)

**Purpose**: FR-028 (deuda de la revisión 001), FR-026 (Swagger), E2E, documentación pendiente de la constitución v1.1.0 y validación con el front.

- [X] T054 [P] Crear `src/test/java/com/tractorstore/ArchitectureRules.java` con las reglas como constantes públicas y refactorizar `src/test/java/com/tractorstore/HexagonalArchitectureTests.java` para usarlas: quitar `allowEmptyShould(true)` de `domainIsFrameworkFree`, `webDoesNotUsePersistence`, `repositoriesLiveInPersistence` y `coreDoesNotDependOnInfrastructure`; `domainIsFrameworkFree` cubre `..domain..` y `com.tractorstore.shared.error..`; nuevas reglas: `apiDoesNotExposeInternals` (`..api..` no depende de `..domain..`, `..application..`, `..infrastructure..`; única con `allowEmptyShould(true)` porque `catalog.api` sigue vacío), `entitiesLiveInPersistence` (`@Entity` solo en `com.tractorstore.*.infrastructure.persistence..`), `sharedHasNoEntities` (`noClasses().that().resideInAPackage("com.tractorstore.shared..").should().beAnnotatedWith(Entity.class)`, constitución II, research R11) y `tablesArePrefixedWithModule` (`ArchCondition` propia: `@Table(name)` de toda `@Entity` de `com.tractorstore.<m>...` empieza por `<m>_`, y lo mismo en `@CollectionTable`/`@JoinTable`; `because("regresión .NET: entidades duplicadas entre módulos")`)
- [X] T055 Crear fixtures de violación en `src/test/java/archfixtures/` (**fuera** de `com.tractorstore`: si no, Spring escanearía sus `@Entity`/`@Component` en los IT y `ddl-auto=validate` rompería el arranque; las reglas se construyen por paquete raíz para poder aplicarlas a los fixtures) (una clase o paquete por regla: dominio que importa Spring, web que usa un repositorio, repositorio fuera de persistence, dominio que depende de infraestructura, `api` que expone dominio, `@Entity` fuera de persistence, `@Entity` en un paquete `shared`, tabla sin prefijo) y `src/test/java/com/tractorstore/ArchitectureRulesSelfTests.java` que importa cada fixture con `ClassFileImporter` y comprueba que `rule.check(...)` lanza `AssertionError` (depende de T054)
- [X] T056 Actualizar `src/test/java/com/tractorstore/ApplicationIT.java`: `openApiDocumentHasExactlyTheFiveCatalogPaths` (`/v3/api-docs` contiene exactamente `/api/v1/catalog/home`, `/categories/{filter}`, `/products/{id}`, `/recommendations`, `/stores`, con tag `Catalog`, `summary`, `description` y respuestas de error con esquema `ApiError`, FR-026) y `unknownCatalogRouteReturnsNotFoundError` (`GET /api/v1/catalog/products` → 404 `$.code == "NOT_FOUND"` con `X-Trace-Id`)
- [X] T057 Escribir `src/test/java/com/tractorstore/catalog/CatalogApiIT.java` (`@SpringBootTest(RANDOM_PORT)` + Testcontainers + `RestTestClient`): E2E de los 5 endpoints contra el dataset real (home 2, listados 23/15/8, detalle `AU-07` igual al ejemplo del contrato, recomendaciones `AU-01-SI` y por defecto, 4 tiendas), 404 `PRODUCT_NOT_FOUND`/`CATEGORY_NOT_FOUND`, 405 `POST /home`, 400 `VALIDATION_ERROR` con un SKU de 21 caracteres, `X-Trace-Id` en toda respuesta y reutilizado si llega válido; `@DisplayName("regresión: sin skus responde 200 con la lista por defecto")` repitiendo 100 peticiones (SC-003). Sin aserciones de tiempo (SC-006 se comprueba a mano en T060)
- [X] T058 [P] Documentar el módulo técnico `shared` (pendientes del Sync Impact Report de la constitución v1.1.0): actualizar el resumen de arquitectura de `CLAUDE.md` ("Módulos: …") y `docs/adr/0001-monolito-modular-hexagonal.md` (nota "Actualización 2026-11-20" que remite a la ADR 0009: cinco módulos de dominio + `shared` técnico), sin reescribir la decisión original
- [X] T059 Ejecutar `./mvnw spotless:apply` y `./mvnw verify` en verde; revisar que ningún archivo de `domain/` ni `shared/error/` importa Spring/JPA/Jackson
- [X] T060 Ejecutar quickstart.md §2–§5 y §7 a mano contra `docker compose up -d postgres` + perfil `dev`, incluidos el tiempo de respuesta < 1 s por endpoint (SC-006, solo manual) y arrancar con `CORS_ALLOWED_ORIGINS='*'` para ver que falla
- [X] T061 Validar con el front real sin cambios (quickstart §6, SC-001): `pnpm nx serve shell`, recorrer home, 3 listados, detalle, recomendaciones y tiendas sin errores de red/CORS en consola; guardar capturas como evidencia para el PR

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias. T002 tras T001; T005 tras T003 y T004.
- **Foundational (Phase 2)**: tras Setup. BLOQUEA todas las historias.
- **US5 (Phase 3)**: tras Foundational. Es la base de datos de US1, US2 y US3.
- **US1 (Phase 4)**: tras US5 (los IT y la demo necesitan datos; los tests unitarios/`@WebMvcTest` pueden empezar tras Foundational).
- **US2 (Phase 5)**: tras US1 (amplía `CatalogController`, `CatalogControllerTest` y `CatalogDtoMapper`, mismos archivos).
- **US4 (Phase 6)**: T044 puede empezar tras Foundational; T045 tras US2 (necesita `/products/{id}`).
- **US3 (Phase 7)**: tras US4 (T052 necesita la traducción de `HandlerMethodValidationException` de T046 para el 400 de `skus`). T047/T050 (política de dominio) pueden hacerse en cuanto termine Foundational.
- **Polish (Phase 8)**: tras todas las historias.

### User Story Dependencies

```text
Setup → Foundational → US5 → US1 → US2 → US4 → US3 → Polish
```

- **US5 (P1)**: independiente; se prueba con `CatalogPersistenceIT`.
- **US1 (P1)**: depende de US5 para datos reales.
- **US2 (P1)**: depende de US1 solo por compartir archivos web.
- **US4 (P2)**: su test principal usa un controller de prueba propio; los casos de catálogo esperan a US2.
- **US3 (P2)**: la política es independiente; el endpoint comparte archivos con US1 y necesita US4 para el 400 de validación.

### Within Each User Story

- Test en rojo → dominio → aplicación → persistencia → web (inside-out, constitución III).
- Una tarea de implementación solo se da por cerrada cuando el test que cita está en verde.
- Commit por tarea o grupo lógico, conventional commits en inglés, sin `Co-Authored-By`.

### Parallel Opportunities

- Setup: T003 ∥ T004 (y T001 en paralelo con ambas).
- Foundational: T006 ∥ T007 ∥ T008 ∥ T009 ∥ T010 ∥ T011; luego T012 ∥ T013 ∥ T014 ∥ T015 ∥ T017.
- US5: T022 ∥ T023; T026 ∥ T027.
- US1: T031 ∥ T032 ∥ T033; T034 ∥ T035.
- US4: T044 en paralelo con US5/US1/US2.
- US3: T047 ∥ T048 (y T047/T050 en paralelo con US5/US1).
- Polish: T054 ∥ T058.

---

## Parallel Example: User Story 1

```bash
# Tests en rojo a la vez:
Task: "GetHomeTeasersServiceTest, GetCategoryListingServiceTest, ListStoresServiceTest en src/test/java/com/tractorstore/catalog/application/service/"
Task: "CatalogControllerTest (@Nested Home, Categories, Stores) en src/test/java/com/tractorstore/catalog/infrastructure/web/controller/CatalogControllerTest.java"
Task: "CorsIT en src/test/java/com/tractorstore/shared/infrastructure/web/CorsIT.java"

# Implementación en paralelo (archivos distintos):
Task: "Casos de uso home/listado/tiendas en src/main/java/com/tractorstore/catalog/application/port/in + service/"
Task: "CatalogPaths + DTOs + CatalogDtoMapper en src/main/java/com/tractorstore/catalog/infrastructure/web/{controller,dto,mapper}/"
```

---

## Implementation Strategy

### MVP First (Setup + Foundational + US5 + US1)

1. Phase 1 → ADR 0008, contrato y módulo `shared`.
2. Phase 2 → dominio, puertos, `traceId` y handler mínimo (con sus tests en rojo primero).
3. Phase 3 (US5) → esquema y dataset; **validar** con `CatalogPersistenceIT`.
4. Phase 4 (US1) → home, listados, tiendas, CORS, puerto 5271.
5. **STOP and VALIDATE**: quickstart §3 y §5; el front pinta la primera pantalla de la demo.

### Incremental Delivery

1. MVP (US5 + US1) → primera pantalla de la demo.
2. + US2 → detalle y elección de SKU (necesario para el carrito de 007).
3. + US4 → errores completos (400/405/404 genérico, `traceId` en logs).
4. + US3 → recomendaciones por color (regresiones .NET) con `skus` validado.
5. Polish → guardas ArchUnit, E2E, Swagger completo, documentación, validación con el front.

---

## Notes

- [P] = archivos distintos y sin dependencias pendientes.
- No editar `V1__baseline.sql` ni, una vez aplicada en cualquier entorno, `V3`: las correcciones van en `V4`.
- `catalog.api` queda vacío en esta spec (lo llena la 007).
- `ErrorKind.CONFLICT` existe pero no se traduce a HTTP hasta la 007.
- Fuera de alcance: inventario (`stock`), carrito, pedidos, caché, reglas ArchUnit de la review 001 #15 (anotadas en la fila 008 del backlog).
