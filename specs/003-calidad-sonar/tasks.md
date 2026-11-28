---

description: "Tareas de implementación de 003 — Calidad con SonarCloud"
---

# Tasks: Calidad con SonarCloud (análisis de PR y Quality Gate obligatorio)

**Input**: Documentos de diseño en `specs/003-calidad-sonar/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/ci-quality-check.md](contracts/ci-quality-check.md),
[quickstart.md](quickstart.md)

**Tests**: esta spec no añade código de producción (ningún fichero bajo `src/main/` cambia; bajo
`src/test/` solo cambia `GlobalExceptionHandlerTest`: `traceId` fijo en `d79f2c9`, C21, y su comentario, N08), así que no hay tareas de
test unitario/integración nuevas. Las suites existentes alimentan la cobertura y
la validación es de **pipeline**: tareas de verificación con resultado esperado explícito
(quickstart §1–§6) y el PR de prueba de SC-002 (rojo sin tests → verde con tests).

**Organization**: tareas agrupadas por historia de usuario. `pom.xml` y `TESTING.md` se editan
en varias fases: las tareas sobre el mismo fichero **no** llevan `[P]` entre sí.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede hacer en paralelo (ficheros distintos, sin dependencias pendientes)
- **[Story]**: historia a la que pertenece (US1, US2, US3, US4)
- **(dueño)**: acción manual del dueño del repositorio en SonarCloud o GitHub; la IA no la
  ejecuta ni maneja el valor del token

## Path Conventions

Proyecto Maven único en la raíz del repositorio. Ficheros tocados: `pom.xml`,
`.github/workflows/ci.yml`, `docs/github/ruleset-main.json`, `TESTING.md` (nuevo),
`.claude/skills/testing-strategy/SKILL.md`. Nada bajo `src/main/`; bajo `src/test/`, solo
`GlobalExceptionHandlerTest` (`traceId` fijo en `d79f2c9`, C21, y su comentario, N08).

---

## Phase 1: Setup (gobierno y prerrequisitos externos)

**Purpose**: confirmar que se cumple FR-001 y que el servicio externo está listo antes de tocar
`pom.xml` o el workflow.

- [X] T001 Verificar que el gobierno de FR-001 ya está aplicado (commit `6d3a3b6`): `docs/adr/0010-sonarcloud-en-alcance-sonar-way.md` con `Estado: aceptada`; `.specify/memory/constitution.md` en `**Version**: 1.2.0` con SonarCloud fuera de la lista "Fuera de alcance" del principio VII, JaCoCo + SonarCloud en Restricciones técnicas y "Quality Gate de SonarCloud aprobado" en la condición de merge del Flujo; `docs/backlog.md` (§Fuera de alcance) y `docs/requisitos.md` (fila SonarCloud → ADR 0010) actualizados. Si falta algo, completarlo en esos ficheros antes de seguir
- [X] T002 (dueño) Confirmar en SonarCloud y GitHub los prerrequisitos de quickstart.md §Prerrequisitos: organización `jesusjs`, proyecto `JesusJs_the-tractor-store-backend-java`, *Automatic Analysis* desactivado, Quality Gate **Sonar way** asignado, *New Code* = "Previous version" (R7), permisos *Administer Issues* y *Administer Security Hotspots* solo para `JesusJs` (R8, FR-004a), secreto de repositorio `SONAR_TOKEN` existente (FR-015; **parcialmente diferido** a la fila 010 del backlog: verificado que es un token personal de `JesusJs`, no un token de alcance mínimo; migrar a un *Scoped Organization Token* queda en la fila 010, review del PR #3, N01) y app de GitHub de SonarCloud instalada en `JesusJs/the-tractor-store-backend-java`

**Checkpoint**: gobierno aceptado y servicio preparado; se puede tocar build y CI.

---

## Phase 2: Foundational (cobertura agregada con JaCoCo)

**Purpose**: un único `target/jacoco.exec` alimentado por surefire **y** failsafe, y un informe
XML/HTML en `verify`. Bloquea US1 (sin informe, Sonar mide 0 % de cobertura y el Quality Gate
fallaría injustamente) y US2.

**⚠️ CRITICAL**: ninguna historia puede empezar hasta terminar esta fase.

- [X] T003 Añadir en `<properties>` de `pom.xml` la propiedad `<jacoco.version>0.8.16</jacoco.version>` (versión exacta; el parent de Spring Boot 4.1.2 no la gestiona, R3)
- [X] T004 Declarar `org.jacoco:jacoco-maven-plugin:${jacoco.version}` en `<build><plugins>` de `pom.xml` con la ejecución `prepare-agent` (id `prepare-agent`, fase `initialize`) configurada con `<destFile>${project.build.directory}/jacoco.exec</destFile>` y `<append>true</append>`, para que la propiedad `argLine` la usen surefire y failsafe sin tocar su configuración actual (`includes`, `<skip>${skipUnitTests}</skip>`). No fijar `<argLine>` en surefire/failsafe (si alguna vez hiciera falta, debe ser `@{argLine} …`, R3)
- [X] T005 Añadir al mismo plugin en `pom.xml` la ejecución `report` (id `report`, fase `verify`, `<dataFile>${project.build.directory}/jacoco.exec</dataFile>`, salida por defecto `target/site/jacoco/` con `jacoco.xml` + `index.html`), declarando el bloque del plugin JaCoCo **después** de `maven-failsafe-plugin` para que en `verify` el informe se genere tras los `*IT` (R3)
- [X] T006 Ejecutar `./mvnw spotless:apply` y `./mvnw clean verify` (Docker en marcha) y comprobar `BUILD SUCCESS`, que existen `target/jacoco.exec`, `target/site/jacoco/jacoco.xml` y `target/site/jacoco/index.html`, y que `ModularityTests`/ArchUnit siguen pasando
- [X] T007 Reproducir la secuencia del CI sin `clean` intermedio (`./mvnw -B clean test` y después `./mvnw -B verify -DskipUnitTests -Dspotless.check.skip=true`) y comprobar en `target/site/jacoco/index.html` que `catalog.application.service.*` (solo unitarios) **y** `catalog.infrastructure.persistence.adapter.CategoryRepositoryAdapter` (solo `CatalogPersistenceIT`) tienen líneas cubiertas: prueba de que `append=true` suma ambas invocaciones (FR-008, SC-003 en local)

**Checkpoint**: cobertura agregada disponible en local y con la secuencia del CI.

---

## Phase 3: User Story 1 - Un PR que no supera el Quality Gate no se puede fusionar (Priority: P1) 🎯 MVP

**Goal**: cada PR contra `main` se analiza en SonarCloud desde el job `build`, el resultado del
Quality Gate Sonar way aparece como check del PR y ese check es obligatorio en el ruleset de
`main` junto a `build`.

**Independent Test**: quickstart §5 — PR de prueba con una clase de dominio con lógica (≥ 20
líneas nuevas) sin tests → check de SonarCloud rojo ("Coverage on New Code < 80 %") y merge
bloqueado; añadir tests → verde y merge habilitado.

### Implementation for User Story 1

- [X] T008 [US1] Añadir en `<properties>` de `pom.xml` `<sonar-maven-plugin.version>5.9.0.7382</sonar-maven-plugin.version>` y las propiedades del análisis del contrato: `<sonar.organization>jesusjs</sonar.organization>`, `<sonar.projectKey>JesusJs_the-tractor-store-backend-java</sonar.projectKey>`, `<sonar.host.url>https://sonarcloud.io</sonar.host.url>`, `<sonar.coverage.jacoco.xmlReportPaths>${project.build.directory}/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>`. **Sin** `sonar.token`, `sonar.login` ni `sonar.qualitygate.wait` (FR-014, R4)
- [X] T009 [US1] Declarar `org.sonarsource.scanner.maven:sonar-maven-plugin:${sonar-maven-plugin.version}` en `<build><pluginManagement><plugins>` de `pom.xml` (crear `pluginManagement` si no existe) para que `./mvnw sonar:sonar` use la versión fijada, sin ligarlo a ninguna fase
- [X] T010 [US1] Añadir al final del job `build` de `.github/workflows/ci.yml`, **después** del paso `Package` y sin reordenar los existentes (FR-016), el paso `- name: Quality analysis` con `if: github.event_name == 'pull_request' || github.ref == 'refs/heads/main'`, `env: SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}` y `run: ./mvnw -B sonar:sonar -Dspotless.check.skip=true`. El token nunca en `-D` ni en `run` (FR-014, R4)
- [X] T011 [US1] Comprobar que ningún fichero versionado contiene el token ni propiedades de login: `grep -rn "sonar.token\|sonar.login" pom.xml .github/` da 0 resultados, y `./mvnw -B spotless:check` y `./mvnw -B test` siguen en verde (quickstart §2, SC-005)
- [X] T012 [US1] Commit (`build: add jacoco aggregated coverage and sonarcloud analysis`), push de `003-calidad-sonar` y apertura del PR contra `main` (requiere que `002-catalogo` esté fusionada en `main`; si no, rebasar primero). Verificar en el PR: paso **Quality analysis** en verde sin el valor del token en su log, check de SonarCloud en verde y resumen del Quality Gate en la conversación (quickstart §3, US1-1, SC-004)
- [X] T013 [US1] Anotar el **nombre exacto** del check publicado por la app de SonarCloud en el PR de T012 y su `app_id`/`integration_id` (`gh api repos/JesusJs/the-tractor-store-backend-java/commits/<sha>/check-runs --jq '.check_runs[] | {name, app: .app.id}'`) para el ruleset (R6) → **Resultado**: check `SonarCloud Code Analysis`, app `sonarqubecloud`, `integration_id` `12526` (commit `758ac55`).
- [X] T014 [US1] Actualizar `docs/github/ruleset-main.json`: en la regla `required_status_checks`, mantener `{"context": "build"}` y añadir `{"context": "<nombre exacto de T013>", "integration_id": <id de T013>}`; el resto de reglas sin cambios (FR-006, data-model §Ruleset)
- [X] T015 [US1] (dueño, tras confirmar en chat) Aplicar el JSON al ruleset activo `main-protection` con `gh api -X PUT repos/JesusJs/the-tractor-store-backend-java/rulesets/<ID> --input docs/github/ruleset-main.json` y comprobar con los dos `gh api` de quickstart §4 que los checks obligatorios son `build` + SonarCloud y coinciden con `docs/github/ruleset-main.json` → **Resultado**: aplicado por el dueño al ruleset `27647920`; `gh api repos/JesusJs/the-tractor-store-backend-java/rules/branches/main` devuelve `build` + `SonarCloud Code Analysis` (`integration_id` 12526), igual que `docs/github/ruleset-main.json`. **Desfase detectado** en la review del PR #3 (C11): el `integration_id` 15368 de `build`, añadido al JSON en T034, no llegaba al ruleset activo. Resuelto con el PUT de C11/C12/N06, aplicado por el dueño el 2026-11-25 a las 22:00:14Z; `diff` vacío de `.rules` comprobado el 2026-11-27 (ver `review.md`, decisiones del PR #3).
- [X] T016 [US1] Validación SC-002 (quickstart §5): rama temporal `tmp/sc-002-quality-gate` desde `003-calidad-sonar` con una clase de dominio con lógica de ≥ 20 líneas en `src/main/java/com/tractorstore/catalog/domain/model/` y **sin** tests → PR: check de SonarCloud rojo ("Coverage on New Code < 80 %", no neutral/skipped, FR-007) y merge bloqueado; añadir su test unitario → verde y merge habilitado. Cerrar el PR **sin fusionar** y borrar la rama local y remota; anotar los enlaces de ambas ejecuciones en la descripción del PR de 003 → **Resultado** (PR #4, cerrado sin fusionar; rama borrada): con `PriceRange` sin tests, condición `new_coverage` = **0,0 %** (umbral 80) → gate `ERROR`, check `SonarCloud Code Analysis` en *failure* y `mergeStateStatus` = `BLOCKED` ([run](https://github.com/JesusJs/the-tractor-store-backend-java/actions/runs/45445521831)); con `PriceRangeTest`, `new_coverage` = 100 % → gate `OK` y `mergeStateStatus` = `CLEAN` ([run](https://github.com/JesusJs/the-tractor-store-backend-java/actions/runs/45446150303)).

**Checkpoint**: el Quality Gate bloquea merges en `main` (MVP).

---

## Phase 4: User Story 2 - Cobertura real (unitarios + integración) visible en cada PR (Priority: P2)

**Goal**: la cobertura enviada es la agregada, sin las categorías excluidas, con cada exclusión
justificada en `TESTING.md` y reproducible en local.

**Independent Test**: en SonarCloud → *Measures → Coverage* del PR, `CategoryRepositoryAdapter`
(solo cubierto por `CatalogPersistenceIT`) aparece cubierto y `TractorStoreApplication`, los DTOs
de `infrastructure/web/dto` y `ApiError` no cuentan (`CorsConfiguration` sí cuenta desde T034).

### Implementation for User Story 2

- [X] T017 [US2] Añadir a la ejecución `report` de JaCoCo en `pom.xml` los `<excludes>` de R5, exactamente: `**/infrastructure/web/dto/**`, `**/api/**`, `**/shared/infrastructure/web/ApiError.class`, `**/infrastructure/config/**`, `**/infrastructure/**/*Configuration.class`, `**/TractorStoreApplication.class`. Ninguno debe alcanzar `domain/`, `application/`, `infrastructure/persistence/**`, `web/controller/**`, `web/mapper/**` ni `CorsProperties`, `GlobalExceptionHandler`, `TraceIdFilter` (R5, FR-010) → **Sustituido** por T034 (review #1/#2) y por C01 de la review del PR #3: la lista vigente está en `TESTING.md`
- [X] T018 [US2] Añadir en `<properties>` de `pom.xml` `<sonar.coverage.exclusions>` con los equivalentes en formato Sonar separados por coma: `**/infrastructure/web/dto/**,**/api/**,**/shared/infrastructure/web/ApiError.java,**/infrastructure/config/**,**/infrastructure/**/*Configuration.java,**/TractorStoreApplication.java,src/main/resources/db/migration/**` y `<sonar.cpd.exclusions>src/main/resources/db/migration/**</sonar.cpd.exclusions>` (migraciones fuera de cobertura **y** duplicación, FR-010) → **Sustituido** por T034 (review #1/#2) y por C01 de la review del PR #3: la lista vigente está en `TESTING.md`
- [X] T019 [US2] Ejecutar `./mvnw clean verify` y comprobar en `target/site/jacoco/index.html` que solo quedan fuera `TractorStoreApplication`, los 5 DTOs de `catalog/infrastructure/web/dto` (`CategoryResponse`, `ProductDetailResponse`, `ProductItemResponse`, `StoreResponse`, `TeaserResponse`), `ApiError` y `CorsConfiguration`, y que `CorsProperties` y `CategoryRepositoryAdapter` siguen apareciendo (quickstart §1, US2-1/2/3) → **Sustituido** por T034 (review #1/#2) y por C01 de la review del PR #3: la lista vigente está en `TESTING.md`
- [X] T020 [P] [US2] Crear `TESTING.md` en la raíz con las secciones de cobertura (FR-011): (1) qué cubre cada tipo de test (surefire `*Test`/`*Tests`: unitarios + `ModularityTests`/ArchUnit; failsafe `*IT`: integración con PostgreSQL real vía Testcontainers, nunca H2); (2) cómo generar el informe agregado en local (`./mvnw clean verify` → `target/site/jacoco/index.html`, sin necesidad de SonarCloud, US2-4) y cómo funciona el `.exec` único con `append=true`; (3) tabla de exclusiones de R5 (categoría, patrón Sonar, patrón JaCoCo, justificación) y la regla de que cualquier exclusión nueva exige justificación escrita y revisión en el PR; (4) aviso `@{argLine}` si alguien configura `argLine` en surefire/failsafe
- [X] T021 [US2] Tras el merge, comprobar en SonarCloud → *Measures → Coverage* de `main` que `CategoryRepositoryAdapter` (solo cubierto por `CatalogPersistenceIT`) tiene ≥ 1 línea cubierta y que las clases excluidas no figuran → **SC-003 ya demostrado a nivel de proyecto** (review del PR #3, C20): solo con unitarios (`./mvnw test` + `jacoco:report`) las líneas quedan en 80,2 % y los adaptadores de persistencia en 0/9, 0/19 y 0/12; SonarCloud en el PR #3 muestra `line_coverage` 98,6 % y `branch_coverage` 90,3 %, idénticos al JaCoCo agregado con `*IT`, así que SonarCloud ingiere la cobertura de integración. Queda solo la verificación por archivo en `main` (junto a T025). → **Verificado 2026-11-25** en el análisis de `main` (revisión `9439eea`, merge del PR #3, 2026-11-25T22:01:36Z): `CategoryRepositoryAdapter` 9/9 líneas cubiertas (100 %, solo por `CatalogPersistenceIT`); `ApiError`, los DTOs de `infrastructure/web/dto` y `TractorStoreApplication` sin `lines_to_cover` (excluidas); proyecto: `coverage` 97,4 %, `line_coverage` 98,6 %, `branch_coverage` 90,3 % (API `measures/component_tree` de SonarCloud).

**Checkpoint**: la cobertura del Quality Gate es la real y está documentada.

---

## Phase 5: User Story 3 - El revisor ve issues y Quality Gate en el propio PR (Priority: P3)

**Goal**: Oscar ve en el PR el resumen del Quality Gate y los issues sin cuenta en
SonarCloud, y sabe cómo se tratan hotspots y falsos positivos.

**Independent Test**: abrir el enlace del resumen del PR en una ventana privada (sin sesión) y
llegar a la lista de issues nuevos en < 1 minuto (SC-007).

### Implementation for User Story 3

- [X] T022 [US3] Añadir a `TESTING.md` la sección "Revisar un PR" (FR-013, FR-004a): dónde aparece el resumen de la app de SonarCloud en la conversación del PR, qué métricas de código nuevo muestra y cómo llegar al detalle sin cuenta (proyecto público); procedimiento de hotspots y falsos positivos: solo `JesusJs` marca un hotspot como *Safe* o un issue como *False Positive*/*Won't Fix*, y por cada marca deja un comentario en el PR con enlace y justificación que Oscar valida antes de aprobar; no se desactivan reglas globalmente sin documentarlo
- [X] T023 [US3] Verificar US3-1/US3-2 y SC-007 en el PR de 003: abrir el enlace del resumen en una ventana privada del navegador sin sesión de SonarCloud, comprobar que se ven estado del Quality Gate, métricas de código nuevo y lista de issues con fichero, y anotar el resultado en la descripción del PR → **Resultado**: resumen del PR #3 abierto sin sesión (página con "Log in"): se ven el Quality Gate *Passed*, New Issues 0, Accepted issues 0, cobertura "no hay suficientes líneas" (regla < 20) y duplicaciones. Sin issues abiertos, la lista por fichero queda vacía; la API pública `api/issues/search` responde sin autenticación.

**Checkpoint**: la revisión del Quality Gate no depende de otra herramienta ni de credenciales.

---

## Phase 6: User Story 4 - `main` se analiza en cada push (Priority: P3)

**Goal**: cada push a `main` genera un análisis de la rama `main`, que fija la línea base del
código nuevo.

**Independent Test**: tras fusionar el PR de 003, existe en SonarCloud un análisis de `main` con
la fecha del merge (quickstart §6).

### Implementation for User Story 4

- [X] T024 [US4] Añadir a `TESTING.md` la sección "Código nuevo y línea base" (FR-005, R7): en PRs, código nuevo = diff contra `main`; en `main`, *New Code* = "Previous version" y, como la versión Maven no cambia (`0.0.1-SNAPSHOT`), la línea base es el primer análisis de `main` (código de 001/002 no tiene que llegar al 80 %); cómo comprobarlo en *Project Settings → New Code*; el estado de `main` es informativo y lo que bloquea es el PR
- [X] T025 [US4] Tras fusionar el PR de 003, comprobar en el run de `push` a `main` que el paso **Quality analysis** se ejecutó y en SonarCloud (rama `main`) que hay un análisis con la fecha del merge y su estado de Quality Gate (US4-1); comprobar también que un `push` a una rama distinta de `main` no dispara el workflow (contrato §Disparadores, enmendado por C08/N04 de la review del PR #3) → **Verificado 2026-11-25**: el run `push` de `main` 45709345968 (commit `9439eea`) terminó en `success` con el paso **Quality analysis** en `success`; SonarCloud tiene en `main` un análisis de 2026-11-25T22:01:36Z con Quality Gate **OK** (todas las condiciones en verde). El push de `origin/004-inventario-pedidos` (`897fe31`, ya con `on.push.branches: [main]`) no generó ningún run (0 runs para ese SHA); los runs `push` anteriores de ramas de feature son de commits con el disparador antiguo sin filtro de rama.

**Checkpoint**: `main` es la línea base visible del código nuevo.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: documentación transversal, coherencia de la skill y comprobaciones de éxito.

- [X] T026 Añadir a `TESTING.md` la sección "Quality Gate" (FR-004, FR-011): condiciones de Sonar way sobre código nuevo (cobertura ≥ 80 %, duplicación ≤ 3 %, ratings A en fiabilidad/seguridad/mantenibilidad = 0 bugs y 0 vulnerabilidades nuevos, 100 % hotspots revisados); diferencia aceptada 85 % → 80 % por el plan gratuito con enlace a `docs/adr/0010-sonarcloud-en-alcance-sonar-way.md`; regla de cambios pequeños (< 20 líneas nuevas: cobertura y duplicación no aplican); PRs solo de documentación o ficheros excluidos pasan; el análisis solo corre si todas las suites pasan (FR-009)
- [X] T027 Añadir a `TESTING.md` la sección "Operación" (FR-015, R9, edge cases): token inválido o ausente → paso **Quality analysis** rojo y merge bloqueado; caída del servicio → el dueño quita temporalmente el check de SonarCloud del ruleset, comenta la causa en el PR, fusiona y restaura el check (y `docs/github/ruleset-main.json` sigue reflejando el estado normal); PRs desde forks quedan bloqueados (aceptado); creación y rotación de `SONAR_TOKEN` (SonarCloud → *My Account → Security*, token de análisis del proyecto, actualizar el secreto de GitHub, revocar el anterior) **sin incluir ningún valor**
- [X] T028 [P] Actualizar `.claude/skills/testing-strategy/SKILL.md` (FR-012): sustituir "Cobertura objetivo 80% (JaCoCo), excluyendo records DTO, `config` y la clase `Application`" por el umbral del Quality Gate Sonar way (80 % en código nuevo, bloquea el merge vía SonarCloud; sin umbral local adicional), las exclusiones exactas de R5 (incluidas migraciones y `ApiError`; `CorsProperties` y entidades JPA **no** se excluyen) y un enlace a `TESTING.md`
- [X] T029 [P] Enlazar `TESTING.md` desde la sección de pruebas/comandos de `README.md` (una línea) para que el revisor (Oscar) lo encuentre
- [X] T030 Medir SC-006: comparar la duración del job `build` del PR de 003 con la última ejecución del PR de 002 (`gh run list --branch 002-catalogo --workflow CI --limit 1 --json databaseId,createdAt,updatedAt` y lo mismo para `003-calidad-sonar`); si supera +5 min, añadir a `.github/workflows/ci.yml` caché de `~/.sonar/cache` con `actions/cache` (R10). Anotar ambas duraciones en la descripción del PR → **Resultado**: job `build` de 002 (run 45436546313) 1 min 12 s; de 003 (run 45444398931) 2 min 10 s → +58 s, por debajo de +5 min: sin caché de `~/.sonar/cache`.
- [X] T031 Comprobar SC-005 sin manejar el valor del token: el paso **Secret scan** (gitleaks, que recorre todo el historial) del PR está en verde, `grep -rn "sonar.token\|sonar.login\|SONAR_TOKEN=" pom.xml .github/ docs/ TESTING.md` da 0 resultados fuera de la referencia `${{ secrets.SONAR_TOKEN }}` de `.github/workflows/ci.yml`, y el log del paso **Quality analysis** solo muestra `***` donde aparece el secreto. (dueño) Si quiere la búsqueda literal del valor, la hace él en local → **Resultado**: **Secret scan** en verde (run 45444398931); el `grep` da 0 resultados y la única referencia es `${{ secrets.SONAR_TOKEN }}` en `ci.yml:53`; el log de **Quality analysis** (run 45439615276) muestra `SONAR_TOKEN: ***`.
- [X] T032 Ejecutar `./mvnw spotless:apply` y `./mvnw verify` finales y recorrer quickstart.md §1–§6 marcando cada resultado esperado; actualizar `**Status**` de `specs/003-calidad-sonar/spec.md` a implementada cuando todo esté en verde → **Parcial**: `spotless:apply` sin cambios y `./mvnw clean verify` en verde (159 unit + 65 IT); quickstart §1–§5 comprobados; §6 y el cambio de `**Status**` esperan al merge (T021, T025). → **Cerrada 2026-11-25**: quickstart §6 comprobado con T025 y `**Status**` de `spec.md` → Implemented.
- [X] T033 Corrección derivada del primer análisis (regla Sonar S6506, fuera del plan original): endurecer la descarga de gitleaks en `.github/workflows/ci.yml` con `curl --proto '=https' --tlsv1.2` (commit `df621be`)
- [X] T034 Aplicar las correcciones aceptadas de `review.md` (#1, #2, #4, #5–#13, #15, #17): exclusiones de cobertura sin `*Configuration` y con `api` acotado, `integration_id` del check `build` en el ruleset y ajustes de documentación

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias. T002 lo hace el dueño fuera del repo.
- **Foundational (Phase 2)**: depende de T001–T002. **Bloquea** US1 y US2.
- **US1 (Phase 3)**: depende de Foundational. T012 necesita `002-catalogo` en `main`.
  T014–T015 dependen del nombre real del check (T013). T016 depende de T015 (el bloqueo debe
  estar activo para comprobar "merge bloqueado").
- **US2 (Phase 4)**: depende de Foundational; T021 depende de T012 (PR abierto y analizado).
  T017–T018 deberían entrar en el PR antes de T015 para que el primer check obligatorio ya use
  la cobertura correcta.
- **US3 (Phase 5)**: depende de T012 (PR analizado).
- **US4 (Phase 6)**: T024 se puede escribir en cualquier momento; T025 depende del merge del PR
  de 003 (después de todas las demás fases).
- **Polish (Phase 7)**: T026–T029 antes del merge; T030–T032 con el PR en marcha. T025 es la
  última comprobación tras el merge.

### Orden recomendado (un solo desarrollador)

T001 → T002 → T003 → T004 → T005 → T006 → T007 → T008 → T009 → T017 → T018 → T019 → T010 →
T011 → T020 → T022 → T024 → T026 → T027 → T028 → T029 → T012 → T013 → T021 → T023 → T014 →
T015 → T016 → T030 → T031 → T032 → (merge) → T025

### Within Each User Story

- `pom.xml`: T003 → T004 → T005 → T008 → T009 → T017 → T018 (mismo fichero, secuencial).
- `TESTING.md`: T020 → T022 → T024 → T026 → T027 (mismo fichero, secuencial).
- Verificar en local (T006, T007, T019) antes de empujar al PR.

### Parallel Opportunities

- T020 (`TESTING.md`) en paralelo con T008–T009/T017–T018 (`pom.xml`) y T010 (`ci.yml`).
- T010 (`ci.yml`) en paralelo con las tareas de `pom.xml`.
- T028 (skill) y T029 (`README.md`) en paralelo con todo lo demás.
- T023 (US3) en paralelo con T021 (US2) una vez el PR está analizado.

---

## Parallel Example: User Story 1 + 2

```bash
# Ficheros distintos, sin dependencias entre sí:
Task: "T010 Paso Quality analysis en .github/workflows/ci.yml"
Task: "T020 Crear TESTING.md con secciones de cobertura"
Task: "T028 Actualizar .claude/skills/testing-strategy/SKILL.md"

# Mismo fichero → secuencial:
Task: "T008 Propiedades sonar.* en pom.xml" → "T009 sonar-maven-plugin en pluginManagement de pom.xml"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1 (gobierno ya hecho: solo verificar) + prerrequisitos del dueño.
2. Phase 2: JaCoCo agregado y verificado en local.
3. Phase 3: análisis en el PR, check obligatorio y PR de prueba SC-002.
4. **STOP and VALIDATE**: el merge queda bloqueado con Quality Gate rojo.

Recomendado meter T017–T018 (exclusiones) antes de activar el check obligatorio (T015), para que
el primer Quality Gate exigido ya mida la cobertura real.

### Incremental Delivery

1. Foundational → informe de cobertura agregado en local.
2. US1 → bloqueo de merge (MVP).
3. US2 → exclusiones y documentación de cobertura.
4. US3 → revisión desde el PR documentada y verificada.
5. Polish + merge → US4 (análisis de `main`).

---

## Notes

- Ningún fichero bajo `src/main/` cambia (la clase de T016 vivió solo en una rama temporal ya
  borrada); bajo `src/test/` solo cambia `GlobalExceptionHandlerTest` (`traceId` fijo en `d79f2c9`, C21, y su
  comentario, N08).
- El valor de `SONAR_TOKEN` nunca se lee, escribe ni imprime; solo se referencia como
  `${{ secrets.SONAR_TOKEN }}`.
- T015 modifica una configuración de GitHub compartida: confirmar en chat antes de ejecutarlo.
- Commits en inglés con conventional commits y sin líneas de atribución.
