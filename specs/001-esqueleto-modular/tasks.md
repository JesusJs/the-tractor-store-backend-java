---

description: "Task list for 001 esqueleto-modular"
---

# Tasks: Esqueleto modular del backend

**Input**: Design documents from `/specs/001-esqueleto-modular/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/operational-endpoints.md, quickstart.md

**Tests**: OBLIGATORIOS. La constitución (principio III, NO NEGOCIABLE) exige que cada pieza empiece con un test en rojo antes del código que lo pone en verde. En cada fase el test va primero y se marca 🔴 → 🟢.

**Organization**: tareas agrupadas por historia de usuario para implementar y verificar cada una por separado.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede hacer en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: historia a la que pertenece (US1…US5)
- Cada tarea indica la ruta exacta del archivo

## Path Conventions

Un único proyecto Maven en la raíz del repositorio (sin `backend/`): código en `src/main/java/com/tractorstore/`, recursos en `src/main/resources/`, tests en `src/test/java/com/tractorstore/`. Antes de escribir POM, configuración o tests, cargar la skill `spring-boot-4-notes` (Boot 4: starters modulares, Testcontainers 2, `RestTestClient`, Jackson 3).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: proyecto Maven que compila, sin módulos ni configuración todavía.

- [X] T001 Generar el Maven wrapper (como lo deja start.spring.io) en `mvnw`, `mvnw.cmd` y `.mvn/wrapper/maven-wrapper.properties` (+ `maven-wrapper.jar`, ya permitido en `.gitignore`)
- [X] T002 Crear `pom.xml`: `groupId` `com.tractorstore`, artifact `tractor-store-backend`, parent `org.springframework.boot:spring-boot-starter-parent` **4.1.2**, `java.version` 21; BOM `org.springframework.modulith:spring-modulith-bom` **2.1.1** en `dependencyManagement`. Dependencias: `spring-boot-starter-webmvc`, `-validation`, `-data-jpa`, `-flyway`, `-actuator`, `spring-modulith-starter-core`, `org.springdoc:springdoc-openapi-starter-webmvc-ui` **3.2.0**, `org.flywaydb:flyway-database-postgresql`, `org.postgresql:postgresql` (runtime). Test: `spring-boot-starter-webmvc-test`, `-data-jpa-test`, `-flyway-test`, `-actuator-test`, `-validation-test`, `spring-boot-testcontainers`, `spring-boot-resttestclient`, `spring-modulith-starter-test`, `org.testcontainers:testcontainers-junit-jupiter`, `org.testcontainers:testcontainers-postgresql` (versión del BOM, 2.0.5), `com.tngtech.archunit:archunit-junit5` **1.5.1**. **NO** añadir `spring-modulith-starter-jpa`, `-actuator`, `-runtime` ni `-observability-*` (research R2)
- [X] T003 Crear `src/main/java/com/tractorstore/TractorStoreApplication.java` (`@SpringBootApplication` + `main`) y comprobar `./mvnw -q -DskipTests compile` en verde

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: plugins de build y configuración común que todas las historias necesitan (la app no arranca sin datasource ni Flyway; los tests no se separan sin surefire/failsafe).

**⚠️ CRITICAL**: ninguna historia empieza hasta terminar esta fase.

- [X] T004 En `pom.xml`, configurar `maven-surefire-plugin` (incluye `**/*Test.java`, `**/*Tests.java`; `<skipTests>${skipUnitTests}</skipTests>` con la propiedad `skipUnitTests` = `false` por defecto) y `maven-failsafe-plugin` (goals `integration-test` + `verify`, incluye `**/*IT.java`) (research R13)
- [X] T005 En `pom.xml`, configurar `com.diffplug.spotless:spotless-maven-plugin` **3.11.0** con `googleJavaFormat` **1.37.0**, `removeUnusedImports` y `formatAnnotations`; ejecución `check` ligada a la fase `verify` (research R7)
- [X] T006 En `pom.xml`, configurar `maven-enforcer-plugin` (versión del parent) en fase `validate` con regla `bannedDependencies` que excluye `com.h2database:*`, `org.hsqldb:*`, `org.apache.derby:*`, `io.zonky.test:*` en todos los scopes (FR-007, research R8)
- [X] T007 [P] Crear `src/main/resources/application.yml` (común): `spring.application.name`, `spring.datasource.url: ${DB_URL}`, `username: ${DB_USER}`, `password: ${DB_PASSWORD}` **sin valores por defecto**; `spring.jpa.hibernate.ddl-auto: validate`; `spring.jpa.open-in-view: false`; `spring.flyway.locations: classpath:db/migration` (data-model.md, "Configuración por perfil")
- [X] T008 [P] Crear `src/main/resources/db/migration/V1__baseline.sql` con **solo un comentario SQL** (sin tablas; el esquema llega en la 002)
- [X] T009 [P] Crear `src/main/resources/application-test.yml` (solo ajustes de logs; el datasource lo aporta `@ServiceConnection`)
- [X] T010 [P] Crear `src/test/java/com/tractorstore/TestcontainersConfiguration.java`: `@TestConfiguration(proxyBeanMethods = false)` con un `@Bean @ServiceConnection` de `org.testcontainers.postgresql.PostgreSQLContainer` sobre la imagen `postgres:17` (paquete de Testcontainers 2.x, research R4)
- [X] T011 Ejecutar `./mvnw spotless:apply` y `./mvnw -q test` (aún sin tests: BUILD SUCCESS) para dejar la base en verde

**Checkpoint**: proyecto compila, formatea y separa unit/IT. Las historias pueden empezar.

---

## Phase 3: User Story 1 - Proyecto base verificable con los cinco módulos (Priority: P1) 🎯 MVP

**Goal**: los cinco módulos `catalog`, `inventory`, `cart`, `order`, `notifications` existen con sus capas, Modulith los detecta sin dependencias entre ellos, y la app arranca publicando OpenAPI y Swagger UI sin rutas de negocio.

**Independent Test**: `./mvnw verify` en verde; `ls target/spring-modulith-docs` muestra exactamente los cinco módulos; `GET /v3/api-docs` → 200 con `paths` vacío; `GET /api/v1/catalog/products` → 404 (quickstart §1).

### Tests for User Story 1 ⚠️ (escribir primero, ver en rojo)

- [X] T012 [US1] 🔴 Crear `src/test/java/com/tractorstore/ModularityTests.java`: `ApplicationModules.of(TractorStoreApplication.class)`; (1) `modules.verify()`; (2) AssertJ: los nombres de módulo son **exactamente** `{catalog, inventory, cart, order, notifications}`; (3) `new Documenter(modules).writeDocumentation()` (→ `target/spring-modulith-docs`). Ejecutar `./mvnw test` y confirmar que falla en la aserción (2) (research R6)
- [X] T013 [US1] 🔴 Crear `src/test/java/com/tractorstore/ApplicationIT.java`: `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@Import(TestcontainersConfiguration.class)`, `@AutoConfigureRestTestClient`, `@ActiveProfiles("test")`, usando `RestTestClient`. Casos US1: `GET /v3/api-docs` → 200 y `paths` vacío (`{}`) sin rutas `/actuator` (FR-019); `GET /swagger-ui/index.html` → 200 HTML; `GET /swagger-ui.html` → 302 hacia `/swagger-ui/index.html`; `GET /api/v1/catalog/products` → 404 (FR-010). Ejecutar `./mvnw verify` y confirmar el rojo esperado (contracts/operational-endpoints.md §OpenAPI y §Lo que NO existe)

### Implementation for User Story 1

Cada módulo recibe 7 `package-info.java` (data-model.md, "Capas por módulo"): raíz con `@ApplicationModule(displayName = "...")`; `api` con `@NamedInterface("api")`; `domain` con Javadoc "Java puro: sin Spring, JPA, Jackson ni Hibernate"; `application`; `infrastructure/web`; `infrastructure/persistence`; `infrastructure/config` (Javadoc con la regla de cada capa).

- [X] T014 [P] [US1] Crear los 7 `package-info.java` del módulo Catalog (`displayName = "Catalog"`) en `src/main/java/com/tractorstore/catalog/{,api/,domain/,application/,infrastructure/web/,infrastructure/persistence/,infrastructure/config/}package-info.java`
- [X] T015 [P] [US1] Crear los 7 `package-info.java` del módulo Inventory (`displayName = "Inventory"`) en `src/main/java/com/tractorstore/inventory/{,api/,domain/,application/,infrastructure/web/,infrastructure/persistence/,infrastructure/config/}package-info.java`
- [X] T016 [P] [US1] Crear los 7 `package-info.java` del módulo Cart (`displayName = "Cart"`) en `src/main/java/com/tractorstore/cart/{,api/,domain/,application/,infrastructure/web/,infrastructure/persistence/,infrastructure/config/}package-info.java`
- [X] T017 [P] [US1] Crear los 7 `package-info.java` del módulo Order (`displayName = "Order"`) en `src/main/java/com/tractorstore/order/{,api/,domain/,application/,infrastructure/web/,infrastructure/persistence/,infrastructure/config/}package-info.java`
- [X] T018 [P] [US1] Crear los 7 `package-info.java` del módulo Notifications (`displayName = "Notifications"`) en `src/main/java/com/tractorstore/notifications/{,api/,domain/,application/,infrastructure/web/,infrastructure/persistence/,infrastructure/config/}package-info.java`
- [X] T019 [US1] 🟢 Ejecutar `./mvnw test`: `ModularityTests` en verde. **Mitigación (research R6)**: si un módulo con solo `package-info.java` no se detecta, añadir su interfaz pública vacía `src/main/java/com/tractorstore/<m>/api/<M>Api.java` (p. ej. `CatalogApi`) en los cinco módulos y repetir. Comprobar en `target/spring-modulith-docs` que hay un diagrama por módulo y ninguna flecha entre ellos (US1-2)
- [X] T020 [US1] 🟢 Añadir en `src/main/resources/application.yml` la configuración mínima de springdoc (rutas por defecto `/v3/api-docs` y `/swagger-ui.html`, `springdoc.show-actuator: false` explícito) y ejecutar `./mvnw verify` hasta que los casos US1 de `ApplicationIT` pasen (research R3)

**Checkpoint**: US1 completa — MVP verificable con `./mvnw verify`.

---

## Phase 4: User Story 2 - El build protege las fronteras de la arquitectura (Priority: P1)

**Goal**: el build falla ante (a) módulo → interior de otro, (b) dominio → framework, (c) controller → repositorio, (d) código sin formato, y (FR-007) BD en memoria.

**Independent Test**: introducir cada violación de la tabla de quickstart §4 (y §3 para formato), ver el fallo con la regla rota en el mensaje, revertir y ver verde.

### Tests for User Story 2 ⚠️

- [X] T021 [US2] 🔴 Crear `src/test/java/com/tractorstore/HexagonalArchitectureTests.java` con `@AnalyzeClasses(packages = "com.tractorstore", importOptions = ImportOption.DoNotIncludeTests.class)` y tres `@ArchTest`, cada uno con `.allowEmptyShould(true)` (research R5): (1) `..domain..` no depende de `org.springframework..`, `jakarta.persistence..`, `com.fasterxml.jackson..`, `tools.jackson..`, `org.hibernate..` (FR-004); (2) `..infrastructure.web..` no depende de `..infrastructure.persistence..` ni de `org.springframework.data.repository..` (FR-005); (3) `..domain..` y `..application..` no dependen de `..infrastructure..`. Ver rojo creando temporalmente `src/main/java/com/tractorstore/catalog/domain/Bad.java` con un campo de tipo `org.springframework.stereotype.Component`; `./mvnw test` debe fallar en la regla (1)

### Implementation / verification for User Story 2

- [X] T022 [US2] 🟢 Borrar `catalog/domain/Bad.java` (`git clean -fd src/`) y comprobar `./mvnw test` en verde con las capas vacías
- [X] T023 [US2] Verificar regla controller → repositorio (US2-3): crear temporalmente `src/main/java/com/tractorstore/catalog/infrastructure/persistence/Repo.java` y `src/main/java/com/tractorstore/catalog/infrastructure/web/BadController.java` que lo usa; `./mvnw test` falla en la regla (2) de `HexagonalArchitectureTests`; revertir y ver verde. Guardar la salida del fallo en `specs/001-esqueleto-modular/pr-evidence.md`
- [X] T024 [US2] Verificar FR-016 / US2-1: crear temporalmente `src/main/java/com/tractorstore/catalog/domain/Probe.java` (clase pública) y `src/main/java/com/tractorstore/cart/application/UsesProbe.java` que lo usa; `./mvnw test` falla en `ModularityTests` con "depends on non-exposed type"; revertir y ver verde. Añadir la salida a `specs/001-esqueleto-modular/pr-evidence.md`
- [X] T025 [US2] Verificar FR-004 (dominio → JPA): repetir T021 con un campo `jakarta.persistence.Entity` en `catalog/domain/Bad.java`; falla la regla (1); revertir. Añadir la salida de T021 y de este caso a `specs/001-esqueleto-modular/pr-evidence.md`
- [X] T026 [US2] Verificar formato (US2-4, FR-006, quickstart §3): quitar la indentación de `src/main/java/com/tractorstore/TractorStoreApplication.java`; `./mvnw spotless:check` y `./mvnw verify` fallan nombrando el archivo; `./mvnw spotless:apply` lo devuelve a verde. Añadir la salida a `specs/001-esqueleto-modular/pr-evidence.md`
- [X] T027 [US2] Verificar FR-007: añadir temporalmente `com.h2database:h2` (scope `test`) a `pom.xml`; `./mvnw test` falla en `maven-enforcer-plugin` (`bannedDependencies`); revertir. Añadir la salida a `specs/001-esqueleto-modular/pr-evidence.md`

**Checkpoint**: las cuatro violaciones de SC-002 (más BD en memoria) hacen fallar el build y revertirlas lo devuelve a verde; evidencia lista para el PR.

---

## Phase 5: User Story 3 - Salud de la aplicación y su base de datos (Priority: P2)

**Goal**: `/actuator/health`, `/liveness` y `/readiness` con solo el estado global; readiness incluye la BD y liveness no; `UP` → 200, `DOWN` → 503; ningún otro endpoint de gestión; fail-fast sin BD al arrancar.

**Independent Test**: quickstart §2 (curl a health y probes; `docker compose stop postgres` → readiness 503, liveness 200) y los tres ITs en verde.

### Tests for User Story 3 ⚠️

- [X] T028 [US3] 🔴 Ampliar `src/test/java/com/tractorstore/ApplicationIT.java` con los casos de salud: `GET /actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` → 200 y cuerpo **exactamente** `{"status":"UP"}` (sin `components` ni `details`, FR-009); `GET /actuator/env`, `/actuator/configprops`, `/actuator/beans`, `/actuator/mappings`, `/actuator/info`, `/actuator/metrics`, `/actuator/modulith` → 404 (contracts/operational-endpoints.md). Ejecutar `./mvnw verify` y confirmar rojo (probes sin habilitar)
- [X] T029 [P] [US3] 🔴 Crear `src/test/java/com/tractorstore/ReadinessWhenDatabaseDownIT.java`: `@SpringBootTest(webEnvironment = RANDOM_PORT)` con **su propio** `PostgreSQLContainer` `postgres:17` (`@Container` estático + `@ServiceConnection`, sin importar `TestcontainersConfiguration`), `RestTestClient`; con la app arrancada detener el contenedor y comprobar `/actuator/health/readiness` → 503 `{"status":"DOWN"}`, `/actuator/health/liveness` → 200 `{"status":"UP"}` y `/actuator/health` → 503 (US3-3, FR-008, research R14)
- [X] T030 [P] [US3] 🔴 Crear `src/test/java/com/tractorstore/StartupFailFastIT.java`: **sin Testcontainers ni `@SpringBootTest`**; obtener un puerto libre con `new ServerSocket(0)` (cerrarlo y conservar el número) y lanzar `new SpringApplicationBuilder(TractorStoreApplication.class)` con `spring.datasource.url=jdbc:postgresql://localhost:<puerto>/tractor`, usuario/contraseña ficticios, `spring.datasource.hikari.connection-timeout=2s`, `server.port=0`; `assertThatThrownBy(builder::run)` y comprobar que la cadena de causas contiene `java.net.ConnectException` (o `org.postgresql.util.PSQLException` de conexión rechazada) (US3-5, FR-013, research R14)

### Implementation for User Story 3

- [X] T031 [US3] 🟢 En `src/main/resources/application.yml` añadir (research R9, data-model.md): `management.endpoints.web.exposure.include: health`; `management.endpoint.health.probes.enabled: true`; `management.endpoint.health.show-details: never`; `management.endpoint.health.show-components: never`; `management.endpoint.health.group.readiness.include: readinessState,db`; `spring.datasource.hikari.connection-timeout: 5s`; `spring.datasource.hikari.validation-timeout: 2s`. No tocar el mapeo de códigos HTTP (el de Boot ya es `UP`→200, `DOWN`→503)
- [X] T032 [US3] 🟢 Ejecutar `./mvnw verify` hasta que `ApplicationIT`, `ReadinessWhenDatabaseDownIT` y `StartupFailFastIT` pasen; si el fallo de `StartupFailFastIT` no es claro o tarda, ajustar solo los timeouts de Hikari de T031

**Checkpoint**: US3 verificable con ITs; las probes están listas para la spec 003.

---

## Phase 6: User Story 4 - Base de datos local con un solo comando (Priority: P2)

**Goal**: `docker compose up -d postgres` deja una BD local idempotente y la app arranca en perfil `dev` sin editar archivos ni secretos en el repo.

**Independent Test**: quickstart §2: `docker compose up -d postgres` dos veces sin error, `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` y `curl -i localhost:8080/actuator/health` → 200 `{"status":"UP"}`.

> Sin test automático propio: los ITs usan su base efímera (spec, Assumptions). La verificación es la guía manual de quickstart §2, cuya salida va a `pr-evidence.md`.

### Implementation for User Story 4

- [X] T033 [P] [US4] Crear `compose.yaml` con el servicio `postgres` (`image: postgres:17`), `ports: "${DB_PORT:-5432}:5432"`, `POSTGRES_DB: ${DB_NAME:-tractor}`, `POSTGRES_USER: ${DB_USER:-tractor}`, `POSTGRES_PASSWORD: ${DB_PASSWORD:-tractor}`, volumen con nombre `tractor-pgdata` y `healthcheck` con `pg_isready` (research R11)
- [X] T034 [P] [US4] Crear `.env.example` con `DB_PORT=5432`, `DB_NAME=tractor`, `DB_USER=tractor`, `DB_PASSWORD=tractor` (valores evidentemente no secretos) y un comentario: "copiar a `.env` solo para sobrescribir" (`.env` ya está en `.gitignore`)
- [X] T035 [P] [US4] Crear `src/main/resources/application-dev.yml`: `spring.datasource.url: ${DB_URL:jdbc:postgresql://localhost:${DB_PORT:5432}/${DB_NAME:tractor}}`, `username: ${DB_USER:tractor}`, `password: ${DB_PASSWORD:tractor}` (data-model.md)
- [X] T036 [P] [US4] Crear `src/main/resources/application-prod.yml`: solo `${DB_URL}`, `${DB_USER}`, `${DB_PASSWORD}` **sin valores por defecto** (si falta una variable, el arranque falla) y `server.forward-headers-strategy: framework` (FR-012, research R10)
- [X] T037 [US4] Validar quickstart §2 completo: `up -d` repetido (US4-2), arranque `dev`, curls de health/probes/api-docs/env/`api/v1`, `docker compose stop postgres` → readiness 503 en < 30 s y liveness 200 (SC-003), fail-fast con la BD parada, y puerto alternativo `DB_PORT=55432`. Añadir la salida a `specs/001-esqueleto-modular/pr-evidence.md`

**Checkpoint**: desarrollo diario posible con un comando; cero credenciales reales.

---

## Phase 7: User Story 5 - Integración continua en cada push y PR (Priority: P3)

**Goal**: workflow de GitHub Actions que en cada push y PR ejecuta, en orden y fallando en la primera etapa rota: secret scan (todo el historial) → format → unit+architecture → integration → package.

**Independent Test**: abrir el PR de `001-esqueleto-modular` y ver el job `build` en verde en < 10 min; un commit sin formato falla en *format*; un secreto de ejemplo falla en *secret scan* con el valor `REDACTED` (quickstart §5).

### Implementation for User Story 5

- [X] T038 [US5] Crear `.github/workflows/ci.yml` (research R12, R13): `name: CI`, `on: push` (todas las ramas) y `pull_request`; `permissions: contents: read`; job `build` en `ubuntu-latest`, sin secretos ni tokens. Pasos con nombres estables, en este orden:
  1. `actions/checkout@v6` con `fetch-depth: 0` (comprobar al implementar que la mayor vigente corre en Node 24);
  2. *Secret scan*: `env: GITLEAKS_SHA256: 551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb`; descargar `https://github.com/gitleaks/gitleaks/releases/download/v8.30.1/gitleaks_8.30.1_linux_x64.tar.gz` como `gitleaks.tar.gz`; `echo "$GITLEAKS_SHA256  gitleaks.tar.gz" | sha256sum --check --strict` **antes** de extraer; `./gitleaks git --redact --verbose --exit-code 1 .`;
  3. `actions/setup-java@v5` (Temurin 21, `cache: maven`);
  4. *Format*: `./mvnw -B spotless:check`;
  5. *Unit + architecture*: `./mvnw -B test`;
  6. *Integration*: `./mvnw -B verify -DskipUnitTests -Dspotless.check.skip=true`;
  7. *Package*: `./mvnw -B package -DskipTests -Dspotless.check.skip=true`
- [X] T039 [US5] Ejecutar gitleaks en local sobre todo el historial (`gitleaks git --redact --verbose .`). Si marca los valores de desarrollo `tractor`, crear `.gitleaks.toml` con `[allowlist] paths = [...]` limitado **solo** a `compose.yaml`, `.env.example` y `src/main/resources/application-dev.yml` (nunca desactivar reglas globalmente, FR-015). Si no marca nada, no crear el archivo
- [X] T040 [US5] Empujar la rama, abrir el PR y comprobar el job `build` en verde y < 10 min (SC-005, US5-1). En ramas desechables comprobar: commit sin formato → falla *Format* y no corre lo siguiente (US5-2); clave AWS de ejemplo **no válida** → falla *Secret scan* mostrando archivo y regla con valor `REDACTED` (US5-4); borrar después esas ramas. Añadir enlaces/salidas a `specs/001-esqueleto-modular/pr-evidence.md`
- [X] T041 [US5] Documentar en `specs/001-esqueleto-modular/pr-evidence.md` el paso manual de FR-018 (GitHub → Settings → Branches → regla para `main` → *Require status checks to pass* → `build`) para que el dueño del repo lo configure y compruebe que un PR en rojo no se puede fusionar (US5-5)

**Checkpoint**: todo cambio futuro queda validado automáticamente.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: documentación y validación final.

- [X] T042 [P] Actualizar `README.md` (FR-017): sustituir "En construcción…" por prerrequisitos (Java 21, Docker en marcha) y los comandos `./mvnw verify`, `./mvnw test`, `./mvnw spotless:apply`, `docker compose up -d postgres`, `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`, variables `DB_*` sobrescribibles con `.env`, y URLs `/actuator/health`, `/swagger-ui.html`, `/v3/api-docs`
- [X] T043 [P] Revisar con `grep` que ningún archivo versionado contiene credenciales distintas de los valores de desarrollo `tractor` (US4-3, SC-006)
- [X] T044 Ejecutar `./mvnw spotless:apply` y `./mvnw verify` completo desde un clon limpio (`git clone` a un directorio temporal) cronometrando de clonar a verde < 15 min sin descargas (SC-001)
- [X] T045 Pasar quickstart.md de punta a punta (§1–§5) y completar `specs/001-esqueleto-modular/pr-evidence.md` como cuerpo del PR (FR-016, SC-002, FR-018)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende de Setup; bloquea todas las historias.
- **US1 (Phase 3)**: depende de Foundational.
- **US2 (Phase 4)**: depende de US1 (necesita los paquetes de los módulos para las violaciones temporales y `ModularityTests` para FR-016).
- **US3 (Phase 5)**: depende de US1 (amplía `ApplicationIT`, que crea T013).
- **US4 (Phase 6)**: T033–T036 solo dependen de Foundational; T037 depende de US3 (comprueba health/readiness).
- **US5 (Phase 7)**: depende de US1 y US2 (el pipeline ejecuta sus tests); T040 idealmente tras US3 y US4 para que el PR esté completo.
- **Polish (Phase 8)**: depende de todas las historias.

### User Story Dependencies

- **US1 (P1)**: tras Foundational. Sin dependencias de otras historias.
- **US2 (P1)**: tras US1.
- **US3 (P2)**: tras US1. Independiente de US2.
- **US4 (P2)**: archivos tras Foundational; validación tras US3.
- **US5 (P3)**: tras US1 + US2.

### Within Each User Story

- Test en rojo (🔴) antes de la implementación que lo pone en verde (🟢) — constitución III.
- `package-info.java` antes de la verificación de módulos; configuración antes de la verificación de ITs.
- Cada violación temporal de US2 se revierte antes de la siguiente.

### Parallel Opportunities

- Phase 2: T007, T008, T009, T010 en paralelo (archivos distintos) tras T004–T006 (mismo `pom.xml`, secuenciales).
- US1: T014–T018 (un módulo cada una) en paralelo.
- US3: T029 y T030 en paralelo (T028 toca `ApplicationIT`).
- US4: T033–T036 en paralelo, y en paralelo con US3.
- US2 es **secuencial** (todas las violaciones comparten árbol de trabajo y se revierten con `git clean`).
- Polish: T042 y T043 en paralelo.

---

## Parallel Example: User Story 1

```bash
# Tras T012 y T013 en rojo, crear los cinco módulos a la vez:
Task: "Crear los 7 package-info.java del módulo Catalog en src/main/java/com/tractorstore/catalog/..."
Task: "Crear los 7 package-info.java del módulo Inventory en src/main/java/com/tractorstore/inventory/..."
Task: "Crear los 7 package-info.java del módulo Cart en src/main/java/com/tractorstore/cart/..."
Task: "Crear los 7 package-info.java del módulo Order en src/main/java/com/tractorstore/order/..."
Task: "Crear los 7 package-info.java del módulo Notifications en src/main/java/com/tractorstore/notifications/..."
```

## Parallel Example: User Story 3 + User Story 4

```bash
Task: "Crear ReadinessWhenDatabaseDownIT en src/test/java/com/tractorstore/ReadinessWhenDatabaseDownIT.java"
Task: "Crear StartupFailFastIT en src/test/java/com/tractorstore/StartupFailFastIT.java"
Task: "Crear compose.yaml"
Task: "Crear .env.example"
Task: "Crear src/main/resources/application-dev.yml"
Task: "Crear src/main/resources/application-prod.yml"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup → Phase 2: Foundational.
2. Phase 3: US1 (🔴 `ModularityTests` + `ApplicationIT` → 🟢 módulos + springdoc).
3. **STOP and VALIDATE**: `./mvnw verify` en verde y cinco módulos en `target/spring-modulith-docs`.

### Incremental Delivery

1. Setup + Foundational → base que compila.
2. US1 → MVP verificable.
3. US2 → guardas de arquitectura con evidencia (completa el P1 de la constitución, principio II).
4. US3 + US4 (en paralelo) → salud y BD local.
5. US5 → CI y PR.
6. Polish → README, clon limpio, evidencia final.

Todo va en una sola rama `001-esqueleto-modular` y un único PR (flujo de `CLAUDE.md`); se puede hacer commit al cerrar cada fase con conventional commits en inglés.

---

## Notes

- [P] = archivos distintos, sin dependencias pendientes.
- Las violaciones temporales de US2 **nunca** se commitean: solo se guarda su salida en `pr-evidence.md`.
- `spring-modulith-starter-jpa` queda fuera a propósito (Complexity Tracking del plan; se añade en la 008).
- Si una coordenada o API de Boot 4 no coincide con lo esperado, consultar la skill `spring-boot-4-notes` antes de improvisar.
