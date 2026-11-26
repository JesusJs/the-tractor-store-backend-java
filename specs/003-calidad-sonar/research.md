# Research: 003 — Calidad con SonarCloud

Fecha: 2026-11-23. Entradas: [spec.md](spec.md), constitución v1.1.0, `.github/workflows/ci.yml`,
`docs/github/ruleset-main.json`, `pom.xml`.

## R1. Quality Gate: Sonar way (limitación del plan gratuito)

- **Decision**: usar el Quality Gate integrado **Sonar way**. Código nuevo: cobertura ≥ 80 %,
  duplicación ≤ 3 %, ratings de fiabilidad/seguridad/mantenibilidad A, 100 % hotspots
  revisados. La diferencia con lo pedido (85 %) se documenta en ADR 0010 como limitación del
  plan; la spec (FR-004, SC-002 y relacionados) ya está ajustada al 80 %.
- **Rationale**: el dueño del repositorio verificó (2026-11-23) que el plan gratuito no permite
  Quality Gates propios. Rating A en fiabilidad/seguridad equivale a 0 bugs/0 vulnerabilidades
  nuevos, así que solo cambia el umbral de cobertura.
- **Alternatives considered**: plan de pago; `jacoco:check` al 85 % (mide otra cosa: global, no
  "código nuevo"); SonarQube autoalojado. Detalle en ADR 0010.
- **Nota**: Sonar way ignora cobertura y duplicación si el código nuevo tiene < 20 líneas. Se
  documenta en ADR 0010 y `TESTING.md`; el PR de prueba de SC-002 debe tener ≥ 20 líneas nuevas.

## R2. Gobierno: ADR 0010 + constitución v1.2.0

- **Decision**: ADR 0010 (redactada como *propuesta* en esta fase) y enmienda MINOR de la
  constitución a v1.2.0, aplicadas como **primeras tareas** de `tasks.md`, antes de tocar
  `pom.xml` o el workflow (FR-001). Texto propuesto:
  - Principio VII, lista "Fuera de alcance": quitar `SonarCloud`.
  - Restricciones técnicas: añadir "JaCoCo (cobertura agregada) · SonarCloud (plan gratuito,
    Quality Gate Sonar way), solo para el backend (ADR 0010)".
  - Flujo de desarrollo: "Un PR solo se fusiona con CI en verde (…) **y el Quality Gate de
    SonarCloud aprobado**".
  - Sync Impact Report con 1.1.0 → 1.2.0.
  - `docs/backlog.md` (§Fuera de alcance) y `docs/requisitos.md` (fila de SonarCloud) se
    actualizan en la misma tarea.
- **Rationale**: la constitución exige ADR + subida de versión para cambiar un principio;
  ampliar el alcance es MINOR.
- **Alternatives considered**: justificar solo en Complexity Tracking — no basta, el principio
  VII nombra SonarCloud explícitamente.

## R3. Cobertura agregada con JaCoCo (un solo fichero `.exec`)

- **Decision**: `jacoco-maven-plugin` **0.8.16** (no lo gestiona el parent de Spring Boot 4.1.2;
  versión fijada). Ejecuciones:
  - `prepare-agent` (fase `initialize`): define la propiedad `argLine`, que usan **tanto**
    surefire como failsafe, ambos escribiendo en `target/jacoco.exec` con `append=true`.
  - `report` (fase `verify`, declarado **después** de failsafe en el `pom.xml`): genera
    `target/site/jacoco/jacoco.xml` + HTML con las exclusiones de R5.
- **Rationale**: el CI ejecuta unit (`./mvnw test`) e integración (`./mvnw verify
  -DskipUnitTests`) en invocaciones separadas y sin `clean`; con `append` el segundo run suma
  al primero, así el informe de `verify` es el agregado (FR-008) sin cambiar el orden de pasos
  (FR-016). En local, `./mvnw clean verify` produce el mismo informe (US2-4). Un único `.exec`
  evita `merge` + `report-aggregate` (este último es para multi-módulo Maven).
- **Alternatives considered**: `prepare-agent` + `prepare-agent-integration` con dos `.exec` y
  `merge` — más configuración para el mismo resultado; Sonar puede leer varios XML, pero
  entonces el informe local no sería el mismo.
- **Riesgo**: si alguien fija `<argLine>` en surefire/failsafe, debe usar `@{argLine} …` o se
  pierde el agente. Se anota en `TESTING.md`.

## R4. Escáner: plugin de Maven en el job `build`, tras empaquetar

- **Decision**: `sonar-maven-plugin` **5.9.0.7382** declarado en `pom.xml` (versión fijada) y un
  paso nuevo **"Quality analysis"** al final del job `build`:
  `./mvnw -B sonar:sonar`, con `SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}` en `env`.
  Condición: `if: github.event_name == 'pull_request' || github.ref == 'refs/heads/main'`.
  Propiedades en `pom.xml`: `sonar.organization=jesusjs`,
  `sonar.projectKey=JesusJs_the-tractor-store-backend-java`, `sonar.host.url=https://sonarcloud.io`,
  `sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml`, exclusiones (R5).
  Sin `sonar.qualitygate.wait`.
- **Rationale**:
  - Mismo job → reutiliza clases compiladas e informe sin artefactos; como los pasos son
    secuenciales, si falla cualquier suite el análisis no se ejecuta (FR-009).
  - Orden existente intacto (FR-016): el paso se añade detrás de `Package`.
  - La condición evita analizar dos veces cada commit de PR (el workflow corre en `push` a
    cualquier rama **y** en `pull_request`) y cubre FR-003 / US4.
  - El escáner detecta solo el contexto de PR en GitHub Actions (rama, base, número), y
    `fetch-depth: 0` ya está para calcular "código nuevo" con blame (FR-005).
  - El resultado del Quality Gate lo publica la **app de GitHub de SonarCloud** como check
    propio del PR y con el resumen en la conversación (FR-006, FR-013); por eso no se usa
    `qualitygate.wait`: haría fallar también `build` y mezclaría dos checks distintos.
  - Token: solo en `env` desde el secreto (GitHub lo enmascara), nunca en `-Dsonar.token` ni
    en ficheros (FR-014). Si falta o es inválido, el escáner falla → `build` rojo y el check
    de Sonar no aparece → merge bloqueado (edge case "falta el token").
- **Alternatives considered**: `SonarSource/sonarqube-scan-action` — SonarSource recomienda el
  plugin de Maven para proyectos Maven (lee binarios y fuentes del pom); job separado — exigiría
  repetir los tests o pasar artefactos y alarga SC-006.

## R5. Exclusiones (FR-010)

> **Enmienda**: esta tabla es la decisión original. T034 (review 003 #1 y #2) quitó `*Configuration` y
> acotó `api`, y la review del PR #3 (C01) quitó `api` e `infrastructure/config`. La lista vigente,
> con su justificación, está en [`TESTING.md`](../../TESTING.md#exclusiones-de-cobertura).

- **Decision** (definidas una vez en `pom.xml`, en formato Sonar para el análisis y formato
  JaCoCo para el informe local; tabla con justificación en `TESTING.md`):

  | Categoría | `sonar.coverage.exclusions` | JaCoCo `<excludes>` | Justificación |
  |---|---|---|---|
  | DTOs HTTP | `**/infrastructure/web/dto/**` | `**/infrastructure/web/dto/**` | records sin lógica |
  | Records/eventos de intercambio | `**/api/**` | `**/api/**` | contratos entre módulos (records, interfaces, eventos) |
  | Cuerpo de error | `**/shared/infrastructure/web/ApiError.java` | `**/shared/infrastructure/web/ApiError.class` | record sin lógica |
  | Configuración | `**/infrastructure/config/**`, `**/infrastructure/**/*Configuration.java` | equivalentes `.class` | cableado de Spring; lo ejercitan los `*IT` |
  | Arranque | `**/TractorStoreApplication.java` | `**/TractorStoreApplication.class` | solo `main` |
  | Migraciones | `src/main/resources/db/migration/**` (cobertura **y** `sonar.cpd.exclusions`) | n/a (no es Java) | SQL de esquema/dataset, repetitivo por naturaleza |

- **Garantía** (confirmada por el dueño, 2026-11-23): **ninguna** exclusión alcanza `domain/`,
  `application/` ni los adaptadores (`infrastructure/persistence/**`, `web/controller/**`,
  `web/mapper/**`, filtros y handlers de `shared`). Comprobado aplicando los patrones al árbol de
  `src/main/java`: solo quedan fuera `TractorStoreApplication`, los 5 DTOs de
  `catalog/infrastructure/web/dto`, `ApiError` y `CorsConfiguration`. Por eso `*Configuration`
  se limita a `infrastructure/`.
- **Rationale**: lista cerrada de la spec. `CorsProperties` **no** se excluye: tiene lógica
  (fail-fast ante `*`) y test propio. Las entidades JPA tampoco: se cubren por los `*IT`.
- **Alternatives considered**: excluir `**/*Properties.java` — ocultaría lógica de validación.

## R6. Check obligatorio en el ruleset (FR-006)

- **Decision**: añadir al `required_status_checks` del ruleset `main-protection` el check que
  publica la app de SonarCloud, **con el nombre exacto que aparezca en el primer PR analizado**
  (habitualmente `SonarCloud Code Analysis`; tras el cambio de marca a SonarQube Cloud puede
  variar), fijando su `integration_id` (la app) para que un workflow no pueda suplantarlo.
  Se actualiza en GitHub (`gh api … rulesets`) y en `docs/github/ruleset-main.json`.
- **Rationale**: un nombre de check mal escrito deja el PR bloqueado para siempre; por eso se
  lee del PR real antes de editar el ruleset. Orden: primero el PR de esta spec obtiene un
  análisis verde, luego se exige el check.
- **Alternatives considered**: exigir solo `build` con `qualitygate.wait=true` — R4.

## R7. "Código nuevo" y línea base (FR-005, edge case "primer análisis")

- **Decision**: en PRs, SonarCloud compara con la rama destino (`main`) automáticamente. En
  `main`, definición de código nuevo **"Previous version"** (valor por defecto del proyecto);
  como la versión Maven no cambia (`0.0.1-SNAPSHOT`), el código nuevo de `main` es todo lo
  añadido desde el **primer análisis de `main`**, que fija la línea base (código de 001/002).
  Se documenta en `TESTING.md`; se comprueba en *Project Settings → New Code*.
- **Rationale**: el estado de `main` es informativo (no bloquea); lo que bloquea es el PR, cuyo
  código nuevo es siempre su diff contra `main`.
- **Alternatives considered**: "Number of days" — haría que la línea base de `main` se moviera
  sola; "Reference branch" no aplica a la propia `main`.

## R8. Permisos para hotspots y falsos positivos (FR-004a)

- **Decision**: en SonarCloud, permisos de proyecto *Administer Issues* y *Administer Security
  Hotspots* solo para el dueño (`JesusJs`); cada marca requiere comentario en el PR con enlace y
  justificación. Procedimiento en `TESTING.md`.
- **Rationale**: lo decidió el clarify; es configuración del servicio, no del repo.

## R9. Caída del servicio y PRs desde forks

- **Decision**: procedimiento en `TESTING.md`: el dueño quita temporalmente el check de Sonar
  del ruleset, deja comentario en el PR con la causa y lo restaura tras el merge. PRs desde
  forks: aceptado que queden bloqueados (no hay contribuciones externas).

## R10. Impacto en tiempo (SC-006)

- **Decision**: medir en el PR de esta spec la duración del job `build` frente a la última
  ejecución de 002. Estimación: agente JaCoCo < 10 % en tests, escaneo 1–2 min. Si supera
  5 min, primera palanca: caché de `~/.sonar/cache` con `actions/cache`.

## Sin NEEDS CLARIFICATION pendientes

El nombre exacto del check (R6) se lee del primer análisis real; no bloquea el diseño.
