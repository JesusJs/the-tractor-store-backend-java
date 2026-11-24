# Revisión 003 — 2026-11-24

Rama `003-calidad-sonar` contra `main`. Revisores: `spec-reviewer`, `architecture-reviewer`, `security-reviewer`.
`./mvnw -q verify`: **OK** (exit 0; unit + arquitectura + integración; `target/site/jacoco/jacoco.xml` generado).
Tests de arquitectura (`ModularityTests`, `HexagonalArchitectureTests`, `ArchitectureRulesSelfTests`) en verde.

No hay hallazgos BLOQUEANTES ni ALTOS. Cada hallazgo se contrastó con el código.

| # | Severidad | Revisor | Archivo:línea | Hallazgo | Propuesta |
|---|---|---|---|---|---|
| 1 | MEDIA | arquitectura, spec | pom.xml:35, pom.xml:224; TESTING.md:25 | `**/infrastructure/**/*Configuration` excluye `shared/infrastructure/web/CorsConfiguration`, que tiene lógica de seguridad (orígenes, `allowCredentials(true)`, métodos, headers, `maxAge`). Además contradice TESTING.md:17-18 ("ninguna toca los adaptadores"). `CorsIT` ya la ejercita, así que excluirla no ahorra nada. | Quitar el patrón `*Configuration` de las dos listas y dejar solo `**/infrastructure/config/**`. Actualizar TESTING.md y la skill `testing-strategy`. |
| 2 | MEDIA | spec, arquitectura | pom.xml:35, pom.xml:221; TESTING.md:23 | `**/api/**` es más amplio que FR-010 ("records de intercambio sin lógica"): abarca cualquier paquete `api` a cualquier profundidad y cualquier record con lógica que llegue en 006-008. Hoy `api/` solo tiene `package-info.java`. | Acotar a `com/tractorstore/*/api/**` y añadir en TESTING.md que, si `api/` recibe lógica, se saca de la exclusión. Opcional: regla ArchUnit que exija que `..api..` contenga solo interfaces y records. |
| 3 | MEDIA | seguridad | docs/github/ruleset-main.json:25-29 | El ruleset no exige revisión (0 aprobaciones, `require_code_owner_review: false`). Como las exclusiones y el workflow están en el propio PR, quien tenga escritura puede ampliarlas y fusionar con el gate en verde. Contradice TESTING.md:29-30 ("revisión en el PR"). La regla viene de antes de esta rama, pero esta spec la vuelve relevante. | Exigir ≥ 1 aprobación de code owner al menos para `pom.xml` y `.github/**`. Valorar `require_last_push_approval: true`. |
| 4 | MEDIA | seguridad | docs/github/ruleset-main.json:46 | El check `build` no fija `integration_id`, a diferencia del de SonarCloud. Cualquier integración con permiso sobre statuses podría publicar un `build` verde. | Fijar el `integration_id` de GitHub Actions. Leer el `app.id` real de un check-run, no suponerlo. |
| 5 | MEDIA | spec | docs/backlog.md:11, :22, :40 | El backlog sigue asignando el 003 a `despliegue-azure-esqueleto`, y `calidad-sonar` no tiene fila. "Imprescindible: …003…" y "Secreto en el código → 001 y 003" quedan ambiguos. | Añadir la fila `003-calidad-sonar` (implementada) y renumerar el despliegue. Precisar a qué spec apuntan las líneas 22 y 40. |
| 6 | BAJA | spec, arquitectura | .specify/memory/constitution.md:15 | El Sync Impact Report deja "⚠ Pendiente: skill `testing-strategy`", pero ya se actualizó en esta rama (c43c22a). | Cambiarlo a "✅ Resuelto (spec 003, T028)" sin subir versión. |
| 7 | BAJA | arquitectura | .specify/memory/constitution.md:174 | La secuencia del CI ("formato → tests → arquitectura → build → imagen") no incluye el paso Quality analysis. | Añadir "→ análisis de calidad". |
| 8 | BAJA | spec | TESTING.md:20-27 (tasks.md:107, T020 [X]) | T020 pedía una columna con el patrón JaCoCo; la tabla solo tiene el de Sonar, y las dos listas deben ir sincronizadas. | Añadir la columna JaCoCo (`.class`; "n/a" en migraciones). |
| 9 | BAJA | spec | TESTING.md:40-41 (tasks.md:141, T024 [X]) | La sección "Código nuevo y línea base" está incompleta: falta cómo verificarlo en *Project Settings → New Code*, que la versión es `0.0.1-SNAPSHOT` y que 001/002 no tienen que llegar al 80 %. FR-005 queda documentado solo en parte. | Completar el punto. |
| 10 | BAJA | spec | .claude/skills/testing-strategy/SKILL.md:21 | Dice "Exclusiones exactas", pero lista formas abreviadas que no coinciden literalmente con el pom. | Quitar "exactas" o copiar los patrones literales. |
| 11 | BAJA | spec | specs/003-calidad-sonar/tasks.md:42 | T001 cita el commit `d4d9ba5`, que no es ancestro de HEAD tras el rebase; el equivalente es `6d3a3b6`. | Cambiar la referencia. |
| 12 | BAJA | spec | .github/workflows/ci.yml:25 | El endurecimiento de `curl` (`--proto '=https' --tlsv1.2`, regla S6506) no aparece en tasks.md. | Mencionarlo en tasks.md o en la descripción del PR. |
| 13 | BAJA | arquitectura | CLAUDE.md:6 (Stack) | La guía operativa no menciona JaCoCo, SonarCloud ni el Quality Gate obligatorio. | Añadir una línea en Stack con enlace a TESTING.md y al ADR 0010. Requiere tu aprobación porque toca CLAUDE.md. |
| 14 | BAJA | seguridad | TESTING.md:55-57 | No aclara si el token de *My Account → Security* es personal, con los permisos del dueño de la org. | Confirmar el tipo de token en SonarCloud. Si es personal, valorar una cuenta técnica con solo *Execute Analysis* y documentar la caducidad. |
| 15 | BAJA | seguridad | TESTING.md:59-60 | El plan B (quitar el check a mano) no comprueba que el ruleset activo vuelva a coincidir con el JSON. | Añadir la comprobación con `gh api repos/{owner}/{repo}/rulesets/{id}` frente a `docs/github/ruleset-main.json`. |
| 16 | BAJA | seguridad | .github/workflows/ci.yml:41-54 | `SONAR_TOKEN` se inyecta en el mismo job donde antes corre código del PR (tests, plugins, `mvnw`). Mitigado: el token es de paso, el evento es `pull_request` y los permisos son `contents: read`. | Aceptable para la demo. Alternativa: un job `sonar` aparte con `needs: build` que reciba `target/` como artifact. |
| 17 | BAJA | arquitectura | pom.xml:206-207 | `append=true` sobre un `.exec` fijo acumula ejecuciones locales si no se hace `clean`. No afecta a CI. | Opcional: reforzar el aviso de `clean verify` en TESTING.md. |

## Descartados o ya planificados
- Acciones fijadas por tag y no por SHA, y `distributionSha256Sum` en el wrapper (seguridad): ya están en la fila 010 del backlog (review 001 #8 y #9) y esta rama no los cambia.

## Pendiente después del merge (declarado en tasks.md, no es un defecto)
- T021 / SC-003: cobertura en SonarCloud con el análisis de `main`. En local ya se ve `CategoryRepositoryAdapter` con 9/9 líneas.
- T025 / US4-1: análisis de `main` y paso *skipped* en un push a otra rama.
- T032: quickstart §6 y `Status` de spec.md:7 a implementada.
- SC-001: solo se puede medir en PRs posteriores.
- Comprobar fuera del repo que `integration_id` 12526 y el nombre "SonarCloud Code Analysis" coinciden con el ruleset activo.

## Decisiones (2026-11-24, JesusJs)

| # | Decisión | Justificación |
|---|---|---|
| 1 | Corregido | Se quita `**/infrastructure/**/*Configuration` de Sonar y de JaCoCo; queda solo `**/infrastructure/config/**`. `CorsConfiguration` tiene lógica de seguridad y la cubre `CorsIT`, así que excluirla no ahorraba nada y ocultaba regresiones. Cobertura local tras el cambio (`./mvnw clean verify`): líneas 98,6 % (349/354), ramas 90,3 %; `CorsConfiguration` 12/12 líneas. |
| 2 | Corregido | `**/api/**` se acota a `com/tractorstore/*/api/**` (Sonar: `src/main/java/com/tractorstore/*/api/**`). La regla "en `api` solo contratos sin lógica; si no, se saca de la exclusión" queda escrita en `TESTING.md`. |
| 3 | Aceptado hasta la demo, diferido a la fila 010 del backlog | Con dos revisores y la demo el 2026-11-25, exigir aprobación de code owner ahora frenaría los merges del último día. Tras la demo se exige en `pom.xml` y `.github/**`. |
| 4 | Corregido | `integration_id: 15368` (app `github-actions`) en el check `build` de `docs/github/ruleset-main.json`, leído de los check-runs del PR 3. Lo aplica el dueño con el PUT del ruleset (no lo ejecuta Claude). |
| 5–13 | Corregidos | Documentación: backlog renumerado, Sync Impact Report y secuencia del CI de la constitución, columna JaCoCo y sección "Código nuevo y línea base" en `TESTING.md`, skill sin "exactas", commit `6d3a3b6` en T001, T033 para el cambio de `curl`, línea de SonarCloud en el Stack de `CLAUDE.md`. |
| 14 | Aceptado hasta la demo, diferido a la fila 010 del backlog | El token actual caduca en diciembre de 2026 (anotado en `TESTING.md`). Tras la demo: token con alcance limitado o cuenta técnica con solo *Execute Analysis*. |
| 15 | Corregido | `TESTING.md` explica cómo comparar el ruleset activo con el JSON (`gh api …/rulesets/<id>`) tras restaurar el check. |
| 16 | Aceptado | El evento es `pull_request`: los forks no reciben secretos, el token es de paso y `permissions: contents: read`. Solo hay colaboradores de confianza con escritura. |
| 17 | Corregido | `TESTING.md` refuerza que el informe local exige `clean` por el `append=true`. |
