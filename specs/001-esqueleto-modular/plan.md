# Implementation Plan: Esqueleto modular del backend

**Branch**: `001-esqueleto-modular` | **Date**: 2026-11-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-esqueleto-modular/spec.md` + `docs/prompts/plan-001.md`

## Summary

Proyecto Maven Spring Boot 4.1.2 / Java 21 que arranca, compila y se verifica desde el día 1. Tiene los cinco módulos (`catalog`, `inventory`, `cart`, `order`, `notifications`) vacíos pero con sus capas hexagonales declaradas. Sus fronteras las vigilan Spring Modulith `verify()` y ArchUnit, el formato lo vigila Spotless y la prohibición de H2 el enforcer. Expone solo Actuator `health` (liveness/readiness, readiness con la BD, sin detalles) y springdoc (`/v3/api-docs`, Swagger UI) sin rutas. La BD local se levanta con `docker compose up -d postgres` (PostgreSQL 17) y los ITs usan Testcontainers con `@ServiceConnection`. La CI de GitHub Actions ejecuta gitleaks sobre todo el historial → formato → unit + arquitectura → integración → build. Detalle de decisiones en [research.md](research.md).

## Technical Context

**Language/Version**: Java 21 (Temurin)

**Primary Dependencies**: Spring Boot 4.1.2 (starters `webmvc`, `validation`, `data-jpa`, `flyway`, `actuator`), Spring Modulith 2.1.1 (`starter-core`), springdoc-openapi 3.2.0, Flyway 12.4 + `flyway-database-postgresql`

**Storage**: PostgreSQL 17 (local con Docker Compose; tests con Testcontainers). Flyway con `V1__baseline.sql` vacía; Hibernate `ddl-auto=validate`

**Testing**: JUnit 5 + AssertJ + Mockito (starters de test de Boot 4), Spring Modulith test, ArchUnit 1.5.1, Testcontainers 2.0.5 (`testcontainers-postgresql`), surefire (`*Test`, `*Tests`) / failsafe (`*IT`)

**Target Platform**: servidor Linux en contenedor (Azure Container Apps llega en la spec 003); desarrollo en Linux con Docker

**Project Type**: web-service (monolito modular, un único desplegable)

**Performance Goals**: `/actuator/health` < 1 s con BD local; readiness → `DOWN` < 30 s tras caer la BD (SC-003)

**Constraints**: cero endpoints de negocio, solo `health` como endpoint de gestión, cero secretos en el repo; CI < 10 min (SC-005); de clonar a verde < 15 min (SC-001)

**Scale/Scope**: 5 módulos vacíos (≈35 `package-info.java`), 1 clase de aplicación, 5 tests, 1 workflow

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo cumple este plan | Estado |
|---|---|---|
| I. Contrato primero | Ningún endpoint de negocio (FR-010). El IT comprueba 404 en `/api/v1/...`. El formato de error único llega en la 004 (Assumptions de la spec); no se inventa nada del contrato. La regla ArchUnit "web no devuelve `@Entity`" se difiere a la 002 por no tener entidades (Assumptions). | ✅ |
| II. Monolito modular + hexagonal | 5 módulos con `@ApplicationModule` y `api` `@NamedInterface("api")`. Capas `domain`, `application`, `infrastructure/{web,persistence,config}`. `ModularityTests` + `HexagonalArchitectureTests` en `./mvnw test`. | ✅ |
| III. Pruebas primero (NO NEGOCIABLE) | Cada pieza tiene su test, que se escribe antes y falla en rojo: `ModularityTests` antes de los paquetes, `HexagonalArchitectureTests` antes de las capas, `ApplicationIT` antes de la configuración de Actuator y springdoc. Testcontainers con PostgreSQL real. Enforcer prohíbe H2. | ✅ |
| IV. Datos | Flyway `V1__baseline.sql`, `ddl-auto=validate` y `open-in-view=false` en todos los perfiles. Sin tablas de negocio. | ✅ |
| V. Eventos internos | Fuera de alcance. Se **difiere** `spring-modulith-starter-jpa` (y su tabla del Event Publication Registry) a la spec que publique el primer evento; ver Complexity Tracking. | ✅ (diferido justificado) |
| VI. Seguridad proporcionada | Sin login. Salud sin detalles; ningún otro endpoint de gestión. Credenciales solo por env (dev con valores evidentemente no secretos). gitleaks sobre todo el historial con `--redact`. CI con `contents: read` y sin secretos. CORS y cookie no aplican aún (sin endpoints de negocio). | ✅ |
| VII. Simplicidad y plazo | Solo las dependencias que pide la spec. Se descartan `modulith-actuator`, `-runtime` y `-observability` que propone start.spring.io. Sin JaCoCo todavía. | ✅ |
| VIII. Entregable y operable | `health` con probes de liveness/readiness y perfiles `dev`/`test`/`prod` por env. La imagen Docker no-root llega en la 003 (Assumptions). | ✅ |
| Restricciones técnicas | Java 21, Boot 4.1.x, Modulith 2.x, Maven wrapper, Spotless google-java-format; skill `spring-boot-4-notes` aplicada (starters modulares, Testcontainers 2, `RestTestClient`). | ✅ |
| Flujo | Rama `001-esqueleto-modular`, un PR, conventional commits en inglés. | ✅ |

**Resultado**: pasa. Una desviación respecto al prompt del plan (no incluir `starter-jpa`), justificada abajo. Ninguna violación de la constitución.

**Re-check post-diseño (Fase 1)**: `data-model.md`, `contracts/` y `quickstart.md` no añaden dependencias, endpoints ni tablas. Sigue en ✅.

## Project Structure

### Documentation (this feature)

```text
specs/001-esqueleto-modular/
├── plan.md              # este archivo
├── research.md          # Fase 0: versiones y decisiones verificadas
├── data-model.md        # Fase 1: módulos, capas y perfiles (sin entidades de negocio)
├── quickstart.md        # Fase 1: guía de validación ejecutable
├── contracts/
│   └── operational-endpoints.md   # contrato HTTP de health, OpenAPI y lo que NO existe
├── checklists/          # de /speckit-specify
└── tasks.md             # Fase 2 (/speckit-tasks; no lo crea este comando)
```

### Source Code (repository root)

```text
.
├── pom.xml                          # parent spring-boot-starter-parent 4.1.2; BOM modulith 2.1.1
├── mvnw, mvnw.cmd, .mvn/wrapper/    # Maven wrapper (como lo genera start.spring.io)
├── compose.yaml                     # servicio postgres (postgres:17), volumen, healthcheck
├── .env.example                     # DB_PORT, DB_NAME, DB_USER, DB_PASSWORD (valores de desarrollo)
├── .gitignore                       # + .env, target/
├── .github/workflows/ci.yml         # gitleaks (binario con SHA-256 verificado) → spotless → test → IT → package
├── README.md                        # prerrequisitos y comandos (FR-017)
└── src/
    ├── main/
    │   ├── java/com/tractorstore/
    │   │   ├── TractorStoreApplication.java
    │   │   ├── catalog/
    │   │   │   ├── package-info.java                    # @ApplicationModule(displayName = "Catalog")
    │   │   │   ├── api/package-info.java                # @NamedInterface("api")
    │   │   │   ├── domain/package-info.java             # Java puro (documentado)
    │   │   │   ├── application/package-info.java
    │   │   │   └── infrastructure/
    │   │   │       ├── web/package-info.java
    │   │   │       ├── persistence/package-info.java
    │   │   │       └── config/package-info.java
    │   │   ├── inventory/  (misma forma)
    │   │   ├── cart/       (misma forma)
    │   │   ├── order/      (misma forma)
    │   │   └── notifications/ (misma forma)
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-test.yml
    │       ├── application-prod.yml
    │       └── db/migration/V1__baseline.sql   # solo comentario
    └── test/
        ├── java/com/tractorstore/
        │   ├── ModularityTests.java              # verify() + 5 nombres + Documenter
        │   ├── HexagonalArchitectureTests.java   # ArchUnit (allowEmptyShould)
        │   ├── TestcontainersConfiguration.java  # @ServiceConnection PostgreSQLContainer postgres:17
        │   ├── ApplicationIT.java                # health/probes, api-docs, swagger, 404s
        │   ├── ReadinessWhenDatabaseDownIT.java  # readiness 503 / liveness 200 al caer la BD
        │   └── StartupFailFastIT.java            # sin Testcontainers: puerto cerrado → el contexto no arranca
        └── resources/                            # (vacío por ahora)
```

**Structure Decision**: un solo proyecto Maven en la raíz del repo (sin `backend/`, porque el front vive en otro repositorio). Los tests globales (arquitectura, arranque) están en el paquete raíz `com.tractorstore` porque verifican la aplicación entera. Los tests de cada módulo vivirán en su paquete a partir de la 002. `package-info.java` en cada capa mantiene los directorios en git y documenta su regla.

### Ejecución en `pom.xml` (resumen)

- `maven-enforcer-plugin` (`validate`): `bannedDependencies` H2/HSQLDB/Derby/zonky (FR-007).
- `maven-surefire-plugin`: `*Test`, `*Tests`; `<skip>${skipUnitTests}</skip>` (por defecto `false`; `-DskipTests` sigue funcionando).
- `maven-failsafe-plugin`: `integration-test` + `verify` para `*IT`.
- `spotless-maven-plugin` 3.11.0: `googleJavaFormat` 1.36.0 (research R7) + `removeUnusedImports`; `check` en `validate`.
- `spring-boot-maven-plugin`.

### Orden de implementación (inside-out, test primero)

1. Esqueleto Maven (wrapper, POM, `TractorStoreApplication`, `compose.yaml`, perfiles, `V1`). `./mvnw -q dependency:tree` y build en verde.
2. 🔴 `ModularityTests` (aserción de los 5 nombres) → 🟢 paquetes y `package-info.java` de los 5 módulos.
3. 🔴 `HexagonalArchitectureTests`. Se ve en rojo con una violación temporal en `domain` → 🟢 al revertir. Comprobación con `allowEmptyShould`.
4. 🔴 `ApplicationIT` + `TestcontainersConfiguration` → 🟢 configuración de Actuator y springdoc. Luego `ReadinessWhenDatabaseDownIT` (contenedor propio) y `StartupFailFastIT` (sin Testcontainers: puerto cerrado, `SpringApplicationBuilder.run()` debe lanzar excepción).
5. Spotless + enforcer: comprobar que fallan con un archivo sin formato y con H2 añadido, y revertir.
6. `ci.yml` + README. PR con la evidencia de FR-016/SC-002 y el paso manual de FR-018.

## Complexity Tracking

| Desviación | Por qué | Alternativa más simple descartada porque |
|---|---|---|
| No se incluye `spring-modulith-starter-jpa` (lo pedía `docs/prompts/plan-001.md`) | Registra entidades JPA del Event Publication Registry (verificado en el jar 2.1.1). Con `ddl-auto=validate` y `V1` vacía, la app no arrancaría. | Crear ya sus tablas en `V1` adelanta trabajo de la spec de eventos y obliga a mantener DDL sin uso. Se añade, con su migración, en la spec 008 `pedidos-eventos` (anotado en `docs/backlog.md`). |
| `maven-enforcer-plugin` (no está en el prompt) | FR-007 exige que el build falle si entra una BD en memoria. | La revisión manual no hace fallar la verificación. |
| Tres ITs en vez de uno (`ApplicationIT` + readiness + fail-fast) | US3-3 y US3-5 son criterios de aceptación propios y necesitan otro estado de la BD. | Meterlos en `ApplicationIT` rompería el contenedor compartido. |
| gitleaks por binario con SHA-256 fijado, no por `gitleaks-action` | La clarificación exige escanear todo el historial en cada ejecución (la action escanea solo el rango del evento). El checksum fijado en el repo garantiza que el binario descargado es el esperado antes de ejecutarlo. | — |
