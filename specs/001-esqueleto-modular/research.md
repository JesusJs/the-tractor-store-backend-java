# Research — 001 esqueleto-modular

Fase 0 del plan. Versiones verificadas el 2026-11-17 contra start.spring.io (metadatos y `pom.xml` generado), Maven Central y GitHub Releases. Ningún `NEEDS CLARIFICATION` queda abierto.

## R1. Versión de Spring Boot y coordenadas de los starters

- **Decision**: `spring-boot-starter-parent` **4.1.2** (último parche GA de 4.1; start.spring.io lo marca por defecto). Starters: `spring-boot-starter-webmvc`, `-validation`, `-data-jpa`, `-flyway`, `-actuator`; runtime `org.postgresql:postgresql`; `org.flywaydb:flyway-database-postgresql`.
  Test: `spring-boot-starter-webmvc-test`, `-data-jpa-test`, `-flyway-test`, `-actuator-test`, `-validation-test`, `spring-boot-testcontainers`, `spring-boot-resttestclient`.
- **Rationale**: son las coordenadas exactas que genera start.spring.io para Boot 4.1.2. Boot 4 separa la autoconfiguración en módulos, así que cada tecnología tiene su starter y su starter de test (skill `spring-boot-4-notes`, punto 1).
- **Alternatives**: 4.2.0 (está en RC2; la ADR 0006 descarta migrar antes de la demo). `spring-boot-starter-web` (nombre de Boot 3; en 4 se sustituye por `-webmvc`).
- Versiones que fija el BOM de Boot 4.1.2: Flyway 12.4.0, Hibernate 7.4.13.Final, Jackson 3.1.7, PostgreSQL JDBC 42.7.14, Testcontainers 2.0.5.

## R2. Spring Modulith

- **Decision**: BOM `spring-modulith-bom` **2.1.1** (la versión que start.spring.io asocia a Boot 4.1.2), con `spring-modulith-starter-core` y, en test, `spring-modulith-starter-test`. **`spring-modulith-starter-jpa` NO se añade en 001.**
- **Rationale**: inspeccioné `spring-modulith-events-jpa-2.1.1.jar`. Registra entidades JPA (`JpaEventPublication`, `ArchivedJpaEventPublication`) mediante autoconfiguración. Con `ddl-auto=validate` (constitución IV) Hibernate exigiría sus tablas al arrancar, y `V1__baseline.sql` está vacía, así que la app no arrancaría. Las tablas del registro deben crearse con una migración Flyway que copie el DDL exacto de la versión (skill `modular-architecture`). Eso pertenece a la primera spec que publique eventos: la **008 `pedidos-eventos`**, anotada así en `docs/backlog.md`. En 001 los eventos están fuera de alcance (spec, Assumptions).
- **Alternatives**: (a) incluir `starter-jpa` y crear ya las tablas del registro en `V1`: adelanta trabajo de otra spec y contradice "V1 vacía". (b) incluirlo y desactivarlo por propiedad: es configuración muerta. **Desviación respecto a `docs/prompts/plan-001.md`**, que lo pedía; está anotada en Complexity Tracking.
- No se añaden `spring-modulith-actuator`, `-runtime` ni `-observability-*` (start.spring.io los propone): exponen o preparan endpoints de gestión que FR-009 prohíbe, y son YAGNI.

## R3. springdoc-openapi

- **Decision**: `org.springdoc:springdoc-openapi-starter-webmvc-ui` **3.2.0**.
- **Rationale**: es la línea 3.x de springdoc para Boot 4, y su POM padre declara exactamente Boot 4.1.2. Por defecto `springdoc.show-actuator=false`, así que los endpoints de gestión no aparecen en el documento (FR-019). UI en `/swagger-ui.html` (redirige a `/swagger-ui/index.html`), documento en `/v3/api-docs`.
- **Alternatives**: 3.1.1 (compilada contra Boot 4.1.0). 2.x (Boot 3; incompatible).

## R4. Testcontainers 2

- **Decision**: `org.testcontainers:testcontainers-junit-jupiter` y `org.testcontainers:testcontainers-postgresql` (2.0.5, gestionados por el BOM de Boot). Clase `org.testcontainers.postgresql.PostgreSQLContainer` (paquete nuevo de 2.x; el antiguo `org.testcontainers.containers` está deprecado). Imagen `postgres:17`, la misma que `compose.yaml`.
- **Rationale**: Testcontainers 2 renombra artifactIds y paquetes (skill, punto 5). `@ServiceConnection` configura el `DataSource` sin `@DynamicPropertySource`.
- **Patrón**: `TestcontainersConfiguration` (`@TestConfiguration(proxyBeanMethods = false)`) con un `@Bean @ServiceConnection PostgreSQLContainer`, importada por los `*IT`. El IT de "BD cae tras el arranque" usa **su propio contenedor** porque lo detiene a mitad del test y no puede compartirlo.

## R5. ArchUnit y reglas hexagonales

- **Decision**: `com.tngtech.archunit:archunit-junit5` **1.5.1** (última; no la gestiona Boot). `HexagonalArchitectureTests` con `@AnalyzeClasses(packages = "com.tractorstore", importOptions = DoNotIncludeTests.class)`. Reglas:
  1. `..domain..` no depende de `org.springframework..`, `jakarta.persistence..`, `com.fasterxml.jackson..`, `tools.jackson..`, `org.hibernate..` (FR-004).
  2. `..infrastructure.web..` no depende de `..infrastructure.persistence..` ni de `org.springframework.data.repository..` (FR-005).
  3. `..domain..` y `..application..` no dependen de `..infrastructure..`.
- **Rationale**: en 001 las capas están vacías y ArchUnit falla por defecto ante reglas sin clases (`failOnEmptyShould`). Cada regla lleva `.allowEmptyShould(true)` (edge case "capas vacías").
- **Detectabilidad** (SC-002): las reglas se validan introduciendo una violación a propósito y revirtiéndola (quickstart §4). Esa evidencia va en el PR.

## R6. Verificación de módulos

- **Decision**: `ModularityTests` con `ApplicationModules.of(TractorStoreApplication.class)`: (1) `verify()`; (2) aserción explícita de que los nombres de módulo son exactamente `{catalog, inventory, cart, order, notifications}`; (3) `new Documenter(modules).writeDocumentation()` → `target/spring-modulith-docs`.
- **Rationale**: Modulith detecta como módulo cada paquete directo bajo `com.tractorstore`. La aserción (2) cubre el edge case "deben detectarse los cinco aunque estén vacíos".
- **Riesgo y mitigación**: si un paquete con solo `package-info.java` no se detectara como módulo, la aserción (2) fallaría en rojo. En ese caso cada módulo recibe su interfaz pública vacía `api/XxxApi.java`, prevista igualmente por la skill `hexagonal-architecture`.
- **FR-016** (caso `cart` → interno de `catalog`): se demuestra creando temporalmente `catalog.domain.Probe` y usándolo desde `cart.application`. `verify()` falla con "depends on non-exposed type"; se revierte y vuelve a verde. Queda documentado en el PR.

## R7. Formato (Spotless)

- **Decision**: `com.diffplug.spotless:spotless-maven-plugin` **3.11.0** con `googleJavaFormat` **1.37.0**, `removeUnusedImports` y `formatAnnotations`. Ejecución `check` ligada a la fase `verify`. `./mvnw spotless:apply` formatea.
- **Rationale**: google-java-format 2.0.0 salió el 2026-10-30 y es un salto mayor de hace menos de 3 semanas. 1.37.0 es la última 1.x, estable y compatible con Java 21. Ligar `check` a `verify` hace que `./mvnw verify` falle con código sin formato (FR-006). En CI se ejecuta además como etapa propia (`spotless:check`) para respetar el orden de FR-014.
- **Alternatives**: Checkstyle (solo detecta, no formatea).
- **Ajuste en implement (2026-11-17)**: con Spotless 3.11.0, `googleJavaFormat` 1.37.0 lanza `InvocationTargetException` al formatear cualquier Javadoc (el mismo jar fuera de Spotless funciona). 1.36.0 y 1.35.0 funcionan, así que se fija **1.36.0** en el POM. Revisar al subir Spotless.

## R8. Prohibir bases de datos en memoria (FR-007)

- **Decision**: `maven-enforcer-plugin` (versión gestionada por el parent de Boot) con la regla `bannedDependencies`, que excluye `com.h2database:*`, `org.hsqldb:*`, `org.apache.derby:*` y `io.zonky.test:*` en todos los scopes. Se ejecuta en `validate`.
- **Rationale**: sin esta regla FR-007 solo dependería de revisión humana. Con ella `./mvnw test` falla en cuanto alguien añade H2.

## R9. Salud y endpoints de gestión (FR-008, FR-009, FR-013)

- **Decision** (`application.yml`, todos los perfiles):
  - `management.endpoints.web.exposure.include=health` (solo salud; env, configprops, metrics, info, beans y mappings quedan sin exponer → 404).
  - `management.endpoint.health.probes.enabled=true` (grupos `/actuator/health/liveness` y `/readiness` también fuera de Kubernetes).
  - `management.endpoint.health.show-details=never` y `show-components=never` (solo `{"status":"UP"}`).
  - `management.endpoint.health.group.readiness.include=readinessState,db`. `liveness` conserva solo `livenessState` (no depende de la BD).
  - Códigos HTTP: el mapeo por defecto de Boot ya es `UP`→200 y `DOWN`/`OUT_OF_SERVICE`→503. No se toca; un IT lo comprueba.
- **Fail-fast**: Flyway migra durante el arranque y Hikari usa `initialization-fail-timeout=1` por defecto. Sin BD, el contexto no arranca y el proceso termina con error de conexión. Se fija `spring.datasource.hikari.connection-timeout=5s` para que el fallo sea rápido y claro, y `validation-timeout=2s` para que readiness detecte la caída en mucho menos de 30 s (SC-003). El indicador `db` ejecuta la validación en cada consulta.
- **Alternatives**: `spring.sql.init.continue-on-error` o `lazy-initialization` arrancarían degradados, cosa que la spec rechaza.

## R10. Perfiles y configuración sin secretos (FR-011, FR-012)

- **Decision**:
  - `application.yml`: lo común (JPA `ddl-auto=validate`, `open-in-view=false`, Flyway, management, springdoc). Datasource tomado de `DB_URL`, `DB_USER` y `DB_PASSWORD` **sin valores por defecto**.
  - `application-dev.yml`: valores por defecto locales `jdbc:postgresql://localhost:${DB_PORT:5432}/${DB_NAME:tractor}`, usuario y contraseña `tractor`, todos sobrescribibles por variable de entorno.
  - `application-test.yml`: ajustes de test (logs). El datasource lo aporta `@ServiceConnection`.
  - `application-prod.yml`: solo referencias `${...}` sin valores por defecto, más `server.forward-headers-strategy=framework`. Si falta una variable, el arranque falla.
- **Rationale**: FR-012 exige los tres perfiles y que en `prod` todo salga del entorno.

## R11. Base de datos local (FR-011, SC-004)

- **Decision**: `compose.yaml` con el servicio `postgres` (`postgres:17`), `ports: "${DB_PORT:-5432}:5432"`, `POSTGRES_USER/PASSWORD/DB: ${DB_USER:-tractor}` y similares, volumen con nombre `tractor-pgdata` y `healthcheck` con `pg_isready`. `.env.example` versionado; `.env` en `.gitignore`.
- **Rationale**: con valores por defecto en la interpolación, `docker compose up -d postgres` funciona sin crear `.env` (cero ediciones, SC-004). `.env` es solo para sobrescribir. Ejecutar `up -d` de nuevo es idempotente (US4-2). El puerto es configurable (edge case "puerto ocupado").
- **Nota**: las variables de compose y de la app usan los mismos nombres (`DB_PORT`, `DB_USER`...), así que una sola `.env` sirve para ambas.

## R12. Escaneo de secretos (FR-014, FR-015)

- **Decision**: binario de **gitleaks v8.30.1** descargado en CI desde la release oficial (`https://github.com/gitleaks/gitleaks/releases/download/v8.30.1/gitleaks_8.30.1_linux_x64.tar.gz`). Antes de extraerlo y ejecutarlo se **verifica su SHA-256** contra un valor fijado en el workflow (`GITLEAKS_SHA256: 551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb`, tomado de `gitleaks_8.30.1_checksums.txt` de esa release el 2026-11-17) con `echo "$GITLEAKS_SHA256  gitleaks.tar.gz" | sha256sum --check --strict`. Si no coincide, el paso falla antes de ejecutar nada. Después, tras `actions/checkout` con `fetch-depth: 0`: `./gitleaks git --redact --verbose --exit-code 1 .`. Sin exclusiones de partida. Si un valor de desarrollo (`tractor`) se marcara, se añadiría `.gitleaks.toml` con `[allowlist] paths = [...]` limitado a `compose.yaml`, `.env.example` y `application-dev.yml`.
- **Rationale**: en push y PR, `gitleaks-action` escanea solo el rango de commits del evento, y la clarificación exige **todo el historial** en cada ejecución. La CLI sobre un clon completo lo cumple. `--redact` oculta el valor en el log (FR-015). Al ser un repo personal, no hace falta licencia.
- **Por qué el checksum fijado en el workflow**: comparar con el `checksums.txt` descargado de la misma release no protege si la release se manipula, porque ambos vendrían del mismo origen. Fijar el hash en el repo sí lo hace. Para subir de versión hay que cambiar a la vez la URL y el hash, revisados en el PR.
- **Alternatives**: `gitleaks/gitleaks-action@v3` (escaneo por rango, no historial completo). Imagen Docker `ghcr.io/gitleaks/gitleaks` por etiqueta (la etiqueta es mutable; habría que fijar el digest, y el binario con SHA-256 es más directo). TruffleHog (otra herramienta más; el backlog nombra gitleaks).

## R13. Pipeline de CI (FR-014, SC-005)

- **Decision**: `.github/workflows/ci.yml`, `on: push` (todas las ramas) y `pull_request`. Un job `build` con `permissions: contents: read`, en `ubuntu-latest` (trae Docker para Testcontainers). Pasos en orden:
  1. `actions/checkout@v6` (`fetch-depth: 0`).
  2. Escaneo de secretos: descarga de gitleaks → `sha256sum --check` → `gitleaks git --redact` (R12).
  3. `actions/setup-java@v5` (Temurin 21, `cache: maven`).
  4. Formato: `./mvnw -B spotless:check`.
  5. Unitarios + arquitectura: `./mvnw -B test`.
  6. Integración: `./mvnw -B verify -DskipUnitTests -Dspotless.check.skip=true` (failsafe). Los unitarios no se repiten gracias a la propiedad `skipUnitTests` enlazada a `<skip>` de surefire en el POM (así `-DskipTests` mantiene su significado habitual).
  7. Build: `./mvnw -B package -DskipTests -Dspotless.check.skip=true`, que deja el jar.
  Sin secretos de repositorio ni tokens. Los nombres de job y paso son estables para la comprobación obligatoria (FR-018).
- **Versiones de actions**: checkout v6 y setup-java v5 son las líneas mayores vigentes con runtime Node 24 (las últimas release, v6.3.0 y v7.0.1, se comprueban al implementar; se fija la mayor que corra en Node 24).
- **Rationale**: cada etapa es un paso separado, así que el pipeline se detiene en la primera que falle y el log indica cuál (US5-2).
- **Branch protection (FR-018)**: la configura a mano el dueño en GitHub (Settings → Branches → `main` → require status check `build`). Se documenta en el PR.

## R14. Prueba de integración del arranque

- **Decision**: `ApplicationIT`, `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@Import(TestcontainersConfiguration.class)` + `@AutoConfigureRestTestClient`, usando `RestTestClient` (Framework 7). Comprueba `/actuator/health`, `/liveness` y `/readiness` (200 y cuerpo exactamente `{"status":"UP"}`), `/v3/api-docs` (200 y `paths` vacío), `/swagger-ui/index.html` (200), `/actuator/env`, `/actuator/info` y `/actuator/metrics` (404), y `/api/v1/catalog/products` (404).
- **`ReadinessWhenDatabaseDownIT`**: contenedor propio; lo detiene y comprueba `readiness` → 503 `DOWN` y `liveness` → 200 `UP` (Awaitility no hace falta: el indicador `db` consulta en cada petición).
- **`StartupFailFastIT`**: **sin Testcontainers**, sin `@SpringBootTest` y sin importar `TestcontainersConfiguration`. Busca un puerto libre (`new ServerSocket(0)`, lo cierra y conserva el número) y arranca `new SpringApplicationBuilder(TractorStoreApplication.class)` con `spring.datasource.url=jdbc:postgresql://localhost:<puerto>/tractor`, usuario y contraseña ficticios, `spring.datasource.hikari.connection-timeout=2s` y `server.port=0`. Con AssertJ `assertThatThrownBy(...)` comprueba que `run()` lanza excepción y que en su cadena de causas hay un `java.net.ConnectException` (o un `org.postgresql.util.PSQLException` de conexión rechazada). Así queda claro que el contexto no llega a arrancar. Se ejecuta con failsafe (`*IT`) porque arranca el contexto completo, aunque no necesita Docker.
- **Rationale**: `TestRestTemplate` pasa a `spring-boot-resttestclient` en Boot 4 y `RestTestClient` es la API nueva. Si el slice resultara incómodo, la alternativa es `MockMvc` sobre contexto completo; se decide en implement con la documentación de 4.1 delante.
