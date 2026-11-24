# Implementation Plan: Catálogo (persistencia, errores y endpoints de catálogo)

**Branch**: `002-catalogo` | **Date**: 2026-11-19 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-catalogo/spec.md` + `docs/prompts/plan-002-catalogo.md`

## Summary

Primer módulo con código: `catalog`, en capas hexagonales completas. Sirve los 5 endpoints de
`/api/v1/catalog` con la forma exacta que consume el front (no la de `docs/api-contract.md`, que se
reescribe con una ADR). El dominio es Java puro: `Product`, `Variant`, `Category`, `Store` y `Color`
(distancia RGB y promedio), más la política de recomendaciones por color. Los casos de uso leen por
puertos de salida implementados con JPA (entidades inmutables, una sola consulta SQL por petición).
Esquema y dataset oficial del blueprint (23 productos, 47 variantes, 4 tiendas) se cargan con
Flyway `V2`/`V3`; las imágenes son URLs absolutas del CDN oficial. Se añade un módulo abierto
`shared` con la jerarquía de errores, el `@RestControllerAdvice` único, el filtro de `traceId` y
CORS por entorno; el perfil `dev` escucha en el 5271. Se salda la deuda de arquitectura de la
revisión 001 (#4, #7, #10, `@Entity` y prefijos de tabla). Detalle de decisiones en
[research.md](research.md).

## Technical Context

**Language/Version**: Java 21 (Temurin)

**Primary Dependencies**: Spring Boot 4.1.2 (`webmvc`, `validation`, `data-jpa`, `flyway`,
`actuator`), Spring Modulith 2.1.1, springdoc-openapi 3.2.0, Hibernate 7.4.13, Jackson 3.
**Sin dependencias nuevas** en `pom.xml`.

**Storage**: PostgreSQL 17. Flyway `V2__catalog_schema.sql` (tablas `catalog_*`) y
`V3__catalog_dataset.sql`. `ddl-auto=validate`, `open-in-view=false`. Dinero `NUMERIC(10,2)` ↔
`BigDecimal`.

**Testing**: JUnit 5 + AssertJ + Mockito; `@WebMvcTest` + MockMvc + `@MockitoBean`; `@DataJpaTest` +
Testcontainers (`postgres:17.12`); `@SpringBootTest(RANDOM_PORT)` + `RestTestClient`; ArchUnit 1.5.1;
Modulith `verify()`

**Target Platform**: servidor Linux en contenedor; local con Docker Compose

**Project Type**: web-service (monolito modular)

**Performance Goals**: cada endpoint de catálogo < 1 s en local con el dataset completo (SC-006);
una sentencia SQL por petición (FR-024)

**Constraints**: forma JSON exacta del front (tabla de la spec); CORS con credenciales y orígenes
exactos; ningún error fuera del formato único salvo el rechazo CORS (R10); demo el 2026-11-25

**Scale/Scope**: 1 módulo con código (~25 clases de producción), 1 módulo `shared` (~7 clases),
5 endpoints, 2 migraciones, 5 tablas `catalog_*`, ~12 clases de test

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo cumple este plan | Estado |
|---|---|---|
| I. Contrato primero | La ADR 0008 (aprobada el 2026-11-19, FR-025) reescribe la sección Catalog de `docs/api-contract.md` **antes** del código, con los códigos genéricos `NOT_FOUND`/`METHOD_NOT_ALLOWED` y las limitaciones (precio "desde", rechazo CORS 403 con cuerpo de Spring, dependencia del CDN externo de imágenes). Controllers devuelven records de `infrastructure.web.dto` con mapeo explícito. `@WebMvcTest` por endpoint (status + JSONPath + error). Ningún campo inventado: todos salen de `catalog.models.ts` (contrato en [contracts/](contracts/)). | ✅ |
| II. Monolito modular + hexagonal | `catalog` con `api`/`domain`/`application`/`infrastructure{web,persistence,config}`. Controllers → solo casos de uso. Nuevo módulo técnico `shared` (abierto) para errores, `traceId` y CORS → **ADR 0009** y constitución **v1.1.0** (principio II: cinco módulos de dominio + `shared` técnico). `ModularityTests` + reglas ArchUnit reforzadas. | ✅ |
| III. Pruebas primero | Orden dominio → aplicación → persistencia → web → E2E, cada paso con su test en rojo antes (ver "Orden de implementación"). Testcontainers PostgreSQL; enforcer sin H2. Regresiones .NET con `@DisplayName("regresión: …")`: recomendaciones por color (no categoría), sin `skus` → 200, sin migraciones reales, entidades duplicadas. | ✅ |
| IV. Datos | Tablas `catalog_*`, sin FKs a otros módulos. Flyway `V2`/`V3`; `V1` no se toca. `NUMERIC(10,2)`/`BigDecimal`. `ddl-auto=validate` (ahora con test, review 001 #10) y OSIV off. | ✅ |
| V. Eventos internos | No aplica: catálogo no publica ni consume eventos. | ✅ (n/a) |
| VI. Seguridad proporcionada | Bean Validation en toda entrada: `@Size(max = 20)` en path variables y `@Size(max = 50) List<@Size(max = 20) String> skus` en recomendaciones (corrección C2 de `/speckit-analyze`, 2026-11-20); `X-Trace-Id` entrante saneado (regex, 64 chars); 500 sin stacktrace. CORS con orígenes explícitos por `CORS_ALLOWED_ORIGINS`, credenciales, fail-fast ante `*`. Sin secretos nuevos. Cookie `ts_session` no aplica (catálogo no tiene estado). | ✅ |
| VII. Simplicidad y plazo | Sin dependencias nuevas; sin caché; recomendaciones en memoria (47 variantes); URLs de imagen fijas en la migración en vez de propiedad configurable; `catalog.api` vacío hasta la 007. | ✅ |
| VIII. Entregable y operable | Configuración por perfiles y env (`CORS_ALLOWED_ORIGINS`); health intacto. | ✅ |
| Restricciones técnicas | Boot 4.1.2, skill `spring-boot-4-notes` aplicada (paquetes de slices verificados en los jars, `@MockitoBean`, Jackson 3, `RestTestClient`). | ✅ |
| Flujo | Rama `002-catalogo`, un PR, conventional commits; ADRs 0008 y 0009 antes de tocar código. | ✅ |

**Resultado**: pasa. El módulo `shared` ya no es una desviación: la constitución v1.1.0 (2026-11-20,
ADR 0009) lo admite como módulo técnico.

**Re-check post-diseño (Fase 1)**: `data-model.md` añade 5 tablas `catalog_*` sin referencias
cruzadas; `contracts/` solo contiene campos de los modelos TS del front y los códigos ya listados en
el contrato más `NOT_FOUND`/`METHOD_NOT_ALLOWED` (incluidos en la ADR 0008); `quickstart.md` no añade
dependencias. Sigue en ✅.

## Project Structure

### Documentation (this feature)

```text
specs/002-catalogo/
├── plan.md              # este archivo
├── research.md          # Fase 0: dataset, imágenes, JPA, errores, CORS, deuda 001
├── data-model.md        # Fase 1: dominio, tablas catalog_*, entidades JPA, mapeos
├── quickstart.md        # Fase 1: guía de validación ejecutable
├── contracts/
│   ├── catalog-api.md   # 5 endpoints con la forma del front
│   └── errors.md        # formato único, códigos, X-Trace-Id, CORS
├── checklists/          # de /speckit-specify
└── tasks.md             # Fase 2 (/speckit-tasks; no lo crea este comando)
```

### Source Code (repository root)

```text
docs/
├── adr/0008-contrato-catalogo-segun-front.md      # FR-025 (aprobada 2026-11-19)
├── adr/0009-modulo-shared-errores-web.md          # módulo abierto shared
└── api-contract.md                                # sección Catalog reescrita + NOT_FOUND/METHOD_NOT_ALLOWED

src/main/java/com/tractorstore/
├── shared/
│   ├── package-info.java                          # @ApplicationModule(type = OPEN, allowedDependencies = {})
│   ├── error/                                     # Java puro
│   │   ├── BusinessException.java                 # abstract: code, ErrorKind
│   │   ├── ErrorKind.java                         # NOT_FOUND, CONFLICT
│   │   └── NotFoundException.java
│   └── infrastructure/web/
│       ├── ApiError.java                          # record {code, message, details?, traceId}
│       ├── GlobalExceptionHandler.java            # @RestControllerAdvice (tabla R8)
│       ├── TraceIdFilter.java                     # X-Trace-Id + MDC
│       ├── CorsProperties.java                    # tractorstore.cors.allowed-origins, rechaza '*'
│       └── CorsConfiguration.java                 # WebMvcConfigurer /api/**
└── catalog/
    ├── package-info.java                          # allowedDependencies = {"shared"}
    ├── api/package-info.java                      # vacío hasta 007
    ├── domain/
    │   ├── model/
    │   │   ├── Color.java                         # record r,g,b; fromHex, distanceTo, average
    │   │   ├── Variant.java, Product.java         # startPrice, mainImage, skus, brand, description
    │   │   ├── Category.java, CategoryFilter.java # filtro all|classic|autonomous
    │   │   ├── Store.java
    │   │   ├── Recommendation.java                # (product, variant recomendada)
    │   │   └── DomainChecks.java                  # package-private: invariantes comunes
    │   ├── service/
    │   │   └── ColorRecommendationPolicy.java     # FR-010..FR-013
    │   ├── exception/
    │   │   ├── ProductNotFoundException.java      # PRODUCT_NOT_FOUND
    │   │   ├── CategoryNotFoundException.java     # CATEGORY_NOT_FOUND
    │   │   └── VariantNotFoundException.java      # VARIANT_NOT_FOUND (reservado, carrito)
    │   └── port/out/
    │       ├── ProductRepository.java
    │       ├── CategoryRepository.java
    │       └── StoreRepository.java
    ├── application/
    │   ├── port/in/                               # GetHomeTeasersUseCase, GetCategoryListingUseCase,
    │   │                                          # GetProductDetailUseCase, GetRecommendationsUseCase,
    │   │                                          # ListStoresUseCase
    │   ├── service/                               # *Service (package-private) implementan su *UseCase
    │   └── model/
    │       └── CategoryListing.java               # modelo de lectura de GetCategoryListingUseCase
    └── infrastructure/
        ├── web/
        │   ├── controller/
        │   │   ├── CatalogPaths.java              # BASE = "/api/v1/catalog"
        │   │   └── CatalogController.java         # 5 endpoints + @Operation
        │   ├── dto/                               # TeaserResponse, CategoryResponse, ProductItemResponse,
        │   │                                      # ProductDetailResponse, StoreResponse
        │   └── mapper/
        │       └── CatalogDtoMapper.java
        ├── persistence/
        │   ├── entity/                            # CategoryEntity, ProductEntity, VariantEntity, StoreEntity
        │   ├── repository/                        # JpaProductRepository, JpaCategoryRepository, JpaStoreRepository
        │   └── adapter/                           # ProductRepositoryAdapter, CategoryRepositoryAdapter,
        │                                          # StoreRepositoryAdapter
        └── config/

src/main/resources/
├── application.yml          # + logging.pattern.level con traceId, tractorstore.cors.allowed-origins
├── application-dev.yml      # + server.port 5271, CORS_ALLOWED_ORIGINS por defecto (4200-4203)
└── db/migration/
    ├── V2__catalog_schema.sql
    └── V3__catalog_dataset.sql

src/test/java/com/tractorstore/
├── ArchitectureRules.java                 # reglas como constantes (reutilizadas)
├── HexagonalArchitectureTests.java        # sin allowEmptyShould (salvo api), + api, @Entity, prefijos,
│                                          # puertos en application.port.in / domain.port.out
├── ArchitectureRulesSelfTests.java        # cada regla falla con su fixture
├── archfixtures/…                         # una violación por regla (solo test)
├── JpaSchemaValidationTests.java          # ddl-auto=validate en todos los perfiles
├── ApplicationIT.java                     # actualizado: 5 paths en OpenAPI, 404 NOT_FOUND
├── shared/infrastructure/web/
│   ├── GlobalExceptionHandlerTest.java    # @WebMvcTest con controller de prueba (400/404/405/500)
│   ├── TraceIdFilterTest.java             # reutiliza/genera/sanea
│   └── CorsIT.java                        # perfil dev: 4200-4203 sí, otro origen no
└── catalog/
    ├── domain/model/ColorTest.java        # @ParameterizedTest distancia, hex, promedio
    ├── domain/model/ProductTest.java      # price "desde", image, brand, description (R4)
    ├── domain/model/CategoryFilterTest.java   # all / código exacto, sensible a mayúsculas
    ├── domain/exception/CatalogExceptionsTest.java   # code, mensaje y ErrorKind de las 3 excepciones
    ├── domain/service/ColorRecommendationPolicyTest.java   # regresiones .NET
    ├── application/service/*ServiceTest.java   # Mockito sobre los puertos
    ├── infrastructure/web/controller/CatalogControllerTest.java   # @WebMvcTest, un @Nested por endpoint
    ├── infrastructure/persistence/adapter/CatalogPersistenceIT.java  # dataset, orden, 1 sentencia, re-migración
    └── CatalogApiIT.java                  # E2E de los 5 endpoints + X-Trace-Id
```

**Structure Decision**: mismo proyecto Maven de 001. Los tests de un módulo viven en su paquete
(espejo de `src/main`); los de arquitectura y configuración global siguen en la raíz. `catalog.api`
no recibe clases todavía: ningún otro módulo consume catálogo hasta la 007 (carrito, precio) y la
008 (tiendas).

### Orden de implementación (inside-out, test primero)

0. **Documentación**: ADR 0008 + reescritura de `docs/api-contract.md` (Catalog, códigos genéricos
   y limitaciones: precio "desde", 403 de CORS con cuerpo de Spring, riesgo del CDN externo). La
   ADR 0009 y la constitución v1.1.0 ya están hechas (2026-11-20).
1. 🔴 `ColorTest`, `ProductTest` (incluye `description` de `AU-01`, R4), `CategoryFilterTest`,
   `CatalogExceptionsTest`, `ColorRecommendationPolicyTest` → 🟢 dominio de `catalog` y
   `shared.error`. En `tasks.md`, la primera versión de `TraceIdFilterTest` y
   `GlobalExceptionHandlerTest` (404 de negocio, 500, `traceId`) se adelanta a la fase
   Foundational porque todos los endpoints la usan; el paso 4 la completa.
2. 🔴 `*ServiceTest` con puertos mockeados → 🟢 casos de uso.
3. 🔴 `CatalogPersistenceIT` → 🟢 `V2`, `V3`, entidades, repositorios Spring Data y adaptadores.
4. 🔴 `GlobalExceptionHandlerTest` completo (400/404/405/500, `traceId` en el log) → 🟢
   `shared.infrastructure.web`.
5. 🔴 `CatalogControllerTest` → 🟢 controller, DTOs, mapper, `@Operation`.
6. 🔴 `CorsIT` → 🟢 CORS + puerto dev.
7. 🔴 reglas ArchUnit nuevas + `ArchitectureRulesSelfTests` + `JpaSchemaValidationTests` → 🟢
   (quitar `allowEmptyShould`, `allowedDependencies` de `catalog`).
8. 🔴 `CatalogApiIT` + `ApplicationIT` actualizado → 🟢 E2E.
9. Validación con el front real (quickstart §6, SC-001).

## Complexity Tracking

| Desviación | Por qué | Alternativa más simple descartada porque |
|---|---|---|
| Módulo técnico `shared` (resuelto: constitución v1.1.0 + ADR 0009) | FR-019 exige una jerarquía de errores y un único punto de traducción a HTTP reutilizable por inventario, carrito y pedidos; el prompt del plan pide un paquete compartido permitido por Modulith. Se declara `Type.OPEN` y sin dependencias propias. | Ponerlo en `catalog` obligaría a `inventory`/`cart`/`order` a depender de `catalog` solo por los errores; duplicarlo por módulo viola FR-019. |
| Rechazo CORS (403 `Invalid CORS request`) fuera del formato único | Es la respuesta estándar de Spring; el navegador nunca la entrega al front. | Un `CorsProcessor` propio que escriba `ApiError` añade código y un `code` más sin valor para la demo. Se documenta en la ADR 0008. |
| `ArchitectureRulesSelfTests` con *fixtures* (no lo exige FR-028 literalmente) | Sin él, una regla mal escrita pasa en verde (review 001 #4); con `allowEmptyShould(true)` en la regla de `api`, es la única garantía. | La comprobación manual de 001 no entra en el build. |

**Fuera de este plan**: review 001 #15 (`domain → application`, Spring permitido en `application`,
`@Transactional` solo en `application`). FR-028 no lo incluye; anotado en la fila 008 de
`docs/backlog.md` (decisión del 2026-11-20).
