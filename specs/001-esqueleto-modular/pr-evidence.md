# Evidencia del PR — 001 esqueleto-modular

## US2 — El build protege las fronteras de la arquitectura (FR-004, FR-005, FR-006, FR-007, FR-016, SC-002)

Cada violación se introdujo a propósito, se ejecutó el build, se guardó el fallo y se revirtió.
Tras cada reversión, `./mvnw test` volvió a verde (`Tests run: 6, Failures: 0, Errors: 0`).
Ninguna violación se ha commiteado.

Fecha: 2026-11-17 · Java 21 (target) · Spring Boot 4.1.2 · Spring Modulith 2.1.1 · ArchUnit 1.5.1

| Caso | Cambio temporal | Comando | Falla en | Revertido → verde |
|---|---|---|---|---|
| Dominio → Spring (FR-004) | `catalog/domain/Bad.java` con campo `org.springframework.stereotype.Component` | `./mvnw test` | `HexagonalArchitectureTests.domainIsFrameworkFree` | ✅ |
| Controller → repositorio (FR-005) | `catalog/infrastructure/persistence/Repo.java` + `catalog/infrastructure/web/BadController.java` | `./mvnw test` | `HexagonalArchitectureTests.webDoesNotUsePersistence` | ✅ |
| Módulo → interior de otro (FR-003, FR-016) | `catalog/domain/Probe.java` usada desde `cart/application/UsesProbe.java` | `./mvnw test` | `ModularityTests.modulesRespectTheirBoundaries` | ✅ |
| Dominio → JPA (FR-004) | `catalog/domain/Bad.java` con campo `jakarta.persistence.Entity` | `./mvnw test` | `HexagonalArchitectureTests.domainIsFrameworkFree` | ✅ |
| Código sin formato (FR-006) | quitar la indentación de `TractorStoreApplication.java` | `./mvnw spotless:check` y `./mvnw verify` | `spotless-maven-plugin:check` | ✅ (`spotless:apply`) |
| BD en memoria (FR-007) | `com.h2database:h2` scope `test` en `pom.xml` | `./mvnw test` | `maven-enforcer-plugin` (`bannedDependencies`) | ✅ |

### 1. Dominio → Spring (T021)

```text
[ERROR] Tests run: 3, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.262 s <<< FAILURE! -- in com.tractorstore.HexagonalArchitectureTests
[ERROR] com.tractorstore.HexagonalArchitectureTests.domainIsFrameworkFree -- Time elapsed: 0.254 s <<< FAILURE!
Architecture Violation [Priority: MEDIUM] - Rule 'no classes that reside in a package '..domain..' should depend on classes that reside in any package ['org.springframework..', 'jakarta.persistence..', 'com.fasterxml.jackson..', 'tools.jackson..', 'org.hibernate..'], because domain must be plain Java (FR-004)' was violated (1 times):
Field <com.tractorstore.catalog.domain.Bad.component> has type <org.springframework.stereotype.Component> in (Bad.java:0)
[ERROR] Tests run: 6, Failures: 1, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

### 2. Controller → repositorio (T023)

```text
[ERROR] com.tractorstore.HexagonalArchitectureTests.webDoesNotUsePersistence -- Time elapsed: 0.012 s <<< FAILURE!
Architecture Violation [Priority: MEDIUM] - Rule 'no classes that reside in a package '..infrastructure.web..' should depend on classes that reside in any package ['..infrastructure.persistence..', 'org.springframework.data.repository..'], because controllers must only call use cases (FR-005)' was violated (3 times):
Constructor <com.tractorstore.catalog.infrastructure.web.BadController.<init>()> calls constructor <com.tractorstore.catalog.infrastructure.persistence.Repo.<init>()> in (BadController.java:6)
Field <com.tractorstore.catalog.infrastructure.web.BadController.repo> has type <com.tractorstore.catalog.infrastructure.persistence.Repo> in (BadController.java:0)
Method <com.tractorstore.catalog.infrastructure.web.BadController.get()> calls method <com.tractorstore.catalog.infrastructure.persistence.Repo.find()> in (BadController.java:9)
[ERROR] Tests run: 6, Failures: 1, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

### 3. Módulo → interior de otro módulo (T024)

```text
[ERROR] Tests run: 3, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 1.934 s <<< FAILURE! -- in com.tractorstore.ModularityTests
[ERROR] com.tractorstore.ModularityTests.modulesRespectTheirBoundaries -- Time elapsed: 0.075 s <<< ERROR!
org.springframework.modulith.core.Violations:
- Module 'cart' depends on non-exposed type com.tractorstore.catalog.domain.Probe within module 'catalog'!
Field <com.tractorstore.cart.application.UsesProbe.probe> has type <com.tractorstore.catalog.domain.Probe> in (UsesProbe.java:0)
[ERROR] Tests run: 6, Failures: 0, Errors: 1, Skipped: 0
[ERROR] BUILD FAILURE
```

### 4. Dominio → JPA (T025)

```text
[ERROR] com.tractorstore.HexagonalArchitectureTests.domainIsFrameworkFree -- Time elapsed: 0.394 s <<< FAILURE!
Architecture Violation [Priority: MEDIUM] - Rule 'no classes that reside in a package '..domain..' should depend on classes that reside in any package ['org.springframework..', 'jakarta.persistence..', 'com.fasterxml.jackson..', 'tools.jackson..', 'org.hibernate..'], because domain must be plain Java (FR-004)' was violated (1 times):
Field <com.tractorstore.catalog.domain.Bad.entity> has type <jakarta.persistence.Entity> in (Bad.java:0)
[ERROR] Tests run: 6, Failures: 1, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

### 5. Código sin formato (T026)

`./mvnw spotless:check`:

```text
[ERROR] BUILD FAILURE
[ERROR] Failed to execute goal com.diffplug.spotless:spotless-maven-plugin:3.11.0:check (default-cli) on project tractor-store-backend: The following files had format violations:
[ERROR]     src/main/java/com/tractorstore/TractorStoreApplication.java
[ERROR]         @@ -6,7 +6,7 @@
[ERROR]          @SpringBootApplication
[ERROR]          public·class·TractorStoreApplication·{
[ERROR]
[ERROR]         -public·static·void·main(String[]·args)·{
[ERROR]         -SpringApplication.run(TractorStoreApplication.class,·args);
[ERROR]         -}
[ERROR]         +··public·static·void·main(String[]·args)·{
[ERROR]         +····SpringApplication.run(TractorStoreApplication.class,·args);
[ERROR]         +··}
[ERROR]          }
[ERROR] Run 'mvn spotless:apply' to fix these violations.
```

`./mvnw verify` (los tests pasan; el build cae en la fase `verify` por formato):

```text
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- spotless:3.11.0:check (default) @ tractor-store-backend ---
[ERROR] BUILD FAILURE
[ERROR] Failed to execute goal com.diffplug.spotless:spotless-maven-plugin:3.11.0:check (default) on project tractor-store-backend: The following files had format violations:
[ERROR]     src/main/java/com/tractorstore/TractorStoreApplication.java
[ERROR] Run 'mvn spotless:apply' to fix these violations.
```

Tras `./mvnw spotless:apply`: `./mvnw spotless:check` → `BUILD SUCCESS` y `git diff` vacío sobre el archivo.

### 6. BD en memoria (T027)

```text
[INFO] --- enforcer:3.6.3:enforce (ban-in-memory-databases) @ tractor-store-backend ---
[ERROR] BUILD FAILURE
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-enforcer-plugin:3.6.3:enforce (ban-in-memory-databases) on project tractor-store-backend:
[ERROR] Rule 0: org.apache.maven.enforcer.rules.dependency.BannedDependencies failed with message:
[ERROR] In-memory databases are forbidden: tests use PostgreSQL via Testcontainers (constitution III, FR-007).
[ERROR] com.tractorstore:tractor-store-backend:jar:0.0.1-SNAPSHOT
[ERROR]    com.h2database:h2:jar:2.4.240 <--- banned via the exclude/include list
```

### Estado final tras revertir todo

`./mvnw verify`:

```text
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  21.030 s
```

> Nota: `quickstart.md` §4 propone revertir con `git checkout -- src/ && git clean -fd src/`. Mientras haya
> archivos nuevos sin commitear (como `HexagonalArchitectureTests.java` en esta fase) ese `git clean` también
> los borraría; aquí se revirtió borrando solo los archivos temporales.

## US3 + US4 — Salud y BD local (T028–T037)

### ITs (`./mvnw verify`)

```text
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0 -- in com.tractorstore.ApplicationIT
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in com.tractorstore.StartupFailFastIT
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in com.tractorstore.ReadinessWhenDatabaseDownIT
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Rojo previo a T031: `ApplicationIT` (raíz de health) y `ReadinessWhenDatabaseDownIT` (`Status expected:<503> but was:<200>`).
`StartupFailFastIT` pasó sin código nuevo: el fail-fast ya es el comportamiento por defecto de Boot (test de caracterización).

### quickstart §2 (perfil `dev`)

`docker compose up -d postgres` dos veces: la segunda responde `Running` y sale con 0 (US4-2).

BD arriba:

```text
$ curl -i localhost:8080/actuator/health
HTTP/1.1 200
{"groups":["liveness","readiness"],"status":"UP"}
$ curl -i localhost:8080/actuator/health/liveness
HTTP/1.1 200
{"status":"UP"}
$ curl -i localhost:8080/actuator/health/readiness
HTTP/1.1 200
{"status":"UP"}
/v3/api-docs -> 200 ("paths":{}) · /actuator/env -> 404 · /api/v1/catalog/products -> 404
```

`docker compose stop postgres` (primera readiness tras parar: 503 en 5,0 s < 30 s, SC-003):

```text
$ curl -i localhost:8080/actuator/health
HTTP/1.1 503
{"groups":["liveness","readiness"],"status":"DOWN"}
$ curl -i localhost:8080/actuator/health/liveness
HTTP/1.1 200
{"status":"UP"}
$ curl -i localhost:8080/actuator/health/readiness
HTTP/1.1 503
{"status":"DOWN"}
```

`docker compose start postgres` → readiness `200 {"status":"UP"}` en ~1 s.

Fail-fast con la BD parada: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` → exit 1 en 5 s:

```text
o.s.boot.SpringApplication : Application run failed
Caused by: java.net.ConnectException: Connection refused (connect failed)
Caused by: org.postgresql.util.PSQLException: Connection to localhost:5432 refused. ...
```

Puerto alternativo: `DB_PORT=55432 docker compose up -d postgres` (`0.0.0.0:55432->5432/tcp`) y
`DB_PORT=55432 ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` → readiness `200 {"status":"UP"}`.

### Desviaciones respecto a tasks/research

- **`groups` en la raíz de `/actuator/health`**: Boot 4 siempre lista los nombres de grupo en la raíz (no hay propiedad
  para ocultarlo). No son componentes ni detalles (FR-009 se cumple); el test de la raíz comprueba `status` y la
  ausencia de `components`/`details`, y las probes siguen con cuerpo exacto. Nota ¹ en `contracts/operational-endpoints.md`.
- **Timeouts de Hikari en milisegundos** (`5000`/`2000`, no `5s`/`2s`): `spring.datasource.hikari.*` se enlaza
  directamente a los setters `long` de `HikariDataSource`, sin conversión de `Duration`.
- **`StartupFailFastIT` pasa la configuración como argumentos `--...`**: `SpringApplicationBuilder.properties(...)` son
  propiedades por defecto (prioridad mínima) y `${DB_URL}` de `application.yml` las pisaba.

## US5 — Integración continua (T038–T041)

Workflow [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml), job `build`. Run en verde sobre esta rama:
<https://github.com/JesusJs/the-tractor-store-backend-java/actions/runs/44402794550> (1m17s < 10 min, SC-005).

| Paso | Resultado | Duración |
|---|---|---|
| Checkout (full history) | ✓ | 1 s |
| Secret scan | ✓ `gitleaks.tar.gz: OK` → `15 commits scanned` → `no leaks found` | 0 s |
| Set up Java 21 | ✓ | 2 s |
| Format | ✓ | 8 s |
| Unit + architecture | ✓ `Tests run: 6` | 13 s |
| Integration | ✓ surefire `Tests are skipped` · failsafe `Tests run: 16` | 40 s |
| Package | ✓ `Tests are skipped` → jar | 3 s |

- **T039**: gitleaks no marca los valores de desarrollo `tractor` sobre todo el historial, así que **no** se crea `.gitleaks.toml`.

### Pruebas negativas (T040, hechas a mano por el dueño del repo)

**Código sin formato (US5-2)**: rama desechable `tmp/format-test` (commit `a005053`, "test: deliberately unformatted").
Run: <https://github.com/JesusJs/the-tractor-store-backend-java/actions/runs/44411226439> → `failure`.

| Paso | Resultado |
|---|---|
| Checkout (full history) | ✓ |
| Secret scan | ✓ |
| Set up Java 21 | ✓ |
| Format | ✗ |
| Unit + architecture | skipped |
| Integration | skipped |
| Package | skipped |

```text
[ERROR] Failed to execute goal com.diffplug.spotless:spotless-maven-plugin:3.11.0:check (default-cli) on project tractor-store-backend: The following files had format violations:
[ERROR]     src/main/java/com/tractorstore/TractorStoreApplication.java
[ERROR]         @@ -4,7 +4,7 @@
[ERROR]          @SpringBootApplication
[ERROR]         -public····class·TractorStoreApplication·{
```

**Secreto (US5-4)**: gitleaks 8.30.1 en local (`--redact`) detectó un token falso con la regla `github-pat`, y el
valor apareció ocultado como `REDACTED`. Desviación: se comprobó en local y no en el paso *Secret scan* del CI. Es el
mismo binario, la misma versión y los mismos flags que usa el workflow, y no se subió a GitHub ningún commit con un
secreto.

### Protección de rama (FR-018, paso manual del dueño del repo)

1. GitHub → *Settings* → *Branches* → *Add branch ruleset* (o *Add rule*) para `main`.
2. Activar *Require status checks to pass before merging* y seleccionar el check **`build`**.
3. Activar *Require a pull request before merging*.
4. Comprobar: un PR con el check `build` en rojo muestra *Merging is blocked*.

### Clon limpio (T044, SC-001)

`git clone` a un directorio temporal + `./mvnw -B verify` con caché Maven local: **verde en 35 s** (< 15 min).

### Desviaciones respecto a tasks/research

- **Versiones de actions**: `actions/checkout@v7` y `actions/setup-java@v6` (no v6/v5). R13 pedía fijar la mayor
  vigente con runtime Node 24; a 2026-11-18 son v7.0.1 y v6.3.0, ambas `using: node24`, sin cambios que nos afecten.
- **`skipUnitTests` en el POM**: el primer run mostró que *Package* volvía a ejecutar los unitarios pese a
  `-DskipTests`, porque `<skipTests>${skipUnitTests}</skipTests>` fijaba el parámetro y anulaba la propiedad de línea
  de comandos. Ahora la propiedad se enlaza a `<skip>` de surefire: `-DskipUnitTests` salta solo los unitarios y
  `-DskipTests` vuelve a funcionar como en cualquier proyecto Maven.
- **T039 en local**: el escaneo del historial completo no se ejecutó en la máquina local, sino en el paso *Secret scan*
  del CI (sin hallazgos, ver arriba). En local, gitleaks solo se ejecutó para la prueba negativa de US5-4.
