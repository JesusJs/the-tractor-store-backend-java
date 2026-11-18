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
