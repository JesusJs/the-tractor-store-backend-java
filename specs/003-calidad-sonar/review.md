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

## Review de Oscar en el PR #3 (2026-11-25)

Review de `@oscarmoraquind` (CHANGES_REQUESTED) con IDs `C*`/`N*`. Oscar pasa a ser el **único
revisor y code owner**: Steven sale del proyecto. Esto sustituye la decisión #3 de arriba
("diferido a la fila 010"): la aprobación de code owner se exige ya (C12).

| ID | Sev. | Decisión | Qué se hizo | Commit |
|---|---|---|---|---|
| C12 | ALTA | Corregido | `CODEOWNERS` solo con `@oscarmoraquind` (login verificado con `gh api …/collaborators`; `stevenMetiorQuind` no es colaborador). Skill `/pr` con `--reviewer oscarmoraquind`. Menciones a Steven como revisor quitadas de `TESTING.md`, ADR 0010, spec.md y tasks.md (`CLAUDE.md`, la constitución y `practicas-ia.md` no lo mencionaban; se conserva la cita literal del *Input* en spec.md:9). Ruleset JSON: `required_approving_review_count` 1, `require_code_owner_review` true, `dismiss_stale_reviews_on_push` true. | `8d7767a` |
| C11 | MEDIA | Corregido (PUT aplicado por el dueño el 2026-11-25 a las 22:00:14Z) | El JSON ya tenía `integration_id: 15368` en `build`; llegó al ruleset activo `27647920` con el PUT del 2026-11-25 a las 22:00:14Z (comando abajo). Nota de desfase en T015. | `8d7767a`, `ab88c4f` |
| N06 | BAJA | Corregido (PUT aplicado el 2026-11-25 a las 22:00:14Z) | `strict_required_status_checks_policy` true y `required_review_thread_resolution` true en el JSON; aplicados con el mismo PUT. | `8d7767a` |
| C01 | MEDIA | Corregido | Quitados `src/main/java/com/tractorstore/*/api/**` e `**/infrastructure/config/**` de `sonar.coverage.exclusions` y de los `<excludes>` de JaCoCo. Hoy solo contienen `package-info.java`: la cobertura no cambia (98 % de líneas). Se reintroducirán, justificadas, en el PR que cree clases sin lógica. | `77ec18f` |
| N02 | MEDIA | Corregido | Eliminado el párrafo "Regla del paquete `api`" de `TESTING.md`; la tabla y el texto dicen ahora que `api` e `infrastructure/config` no se excluyen. | `77ec18f` |
| C03 | MEDIA | Corregido | Ejecución `check` de JaCoCo en `verify` con `dataFile` `jacoco.exec`, los mismos `<excludes>` que `report` y regla `BUNDLE`/`LINE`/`COVEREDRATIO` ≥ 0,80. Probado: con 0,80 → *All coverage checks have been met*; con 0,99 → *lines covered ratio is 0.98, but expected minimum is 0.99* y BUILD FAILURE. Enmienda en ADR 0010, `TESTING.md` y skill `testing-strategy`. | `77ec18f` |
| N03 | BAJA | Corregido | `sonar.junit.reportPaths` = `surefire-reports,failsafe-reports`; reflejado en el contrato del CI. | `77ec18f`, `b21cd82` |
| C08 | MEDIA | Corregido | `on: push` y `pull_request` solo para `main`; `concurrency` `ci-${{ github.ref }}` que cancela solo en PR (nunca en `main`). Contrato `ci-quality-check.md` (disparadores) y T025 actualizados. Caché de `~/.sonar/cache` no añadida (opcional). | `b21cd82` |
| N04 | BAJA | Corregido | `if:` de *Quality analysis*: PR con `base_ref == 'main'` o push a `refs/heads/main`. | `b21cd82` |
| C22 | BAJA | Corregido | Badge de Quality Gate de SonarCloud en el README y "→ análisis de calidad (SonarCloud)" en la sección de CI. | `b21cd82` |
| C05 | BAJA | Corregido | Fila "organización de Quind → organización personal `jesusjs`" en `docs/requisitos.md` y en ADR 0010 (contexto y tabla de diferencias). | `ab88c4f` |
| C13 | BAJA | Corregido | `TESTING.md`: quitar temporalmente el check por caída de SonarCloud exige comentar la causa y la **aprobación explícita de Oscar en el PR**; además lo refuerza C12. | `ab88c4f` |
| C14 | BAJA | Corregido | Fecha exacta: el token caduca el **2027-02-20** (creado el 2026-11-23); no hace falta rotarlo antes del cierre; recordatorio para rotarlo antes del 2027-02-13. | `ab88c4f` |
| N01 | BAJA | Corregido (FR-015 parcial) | Verificado por el dueño: es un **token personal** de `JesusJs` con sus permisos sobre la organización `jesusjs`. `TESTING.md` lo documenta tal cual; T002 anota que FR-015 queda parcialmente diferido; migrar a un *Scoped Organization Token* va a la fila 010. | `ab88c4f` |
| C15 | BAJA | Corregido, con matiz | Enlace oficial: [Understanding quality gates](https://docs.sonarsource.com/sonarqube-cloud/standards/managing-quality-gates/introduction-to-quality-gates/). Matiz verificado: la página dice que los quality gates propios son de los planes Team y Enterprise, pero no nombra el plan gratuito, y cita dos gates integrados (Sonar way y *Sonar way for agentic AI*). La ADR lo dice así. Además, la página indica que el *fudge factor* de 20 líneas lo puede desactivar un administrador del proyecto: se corrigió "no configurable" en la ADR (se mantiene activo). | `ab88c4f` |
| C16 | BAJA | Corregido | "ni front" → "el front se analiza desde su propio repositorio" en la constitución (**v1.2.1**, PATCH), ADR 0010 y FR-002 de spec.md. | `ab88c4f` |
| C18 | BAJA | Corregido | quickstart §1: sin `*Configuration`; `CorsConfiguration` y `CorsProperties` deben aparecer cubiertas; añadido el mensaje de `jacoco:check`. | `ab88c4f` |
| C19 | BAJA | Corregido | T017–T019 anotadas como sustituidas por T034 y C01; quitado `CorsConfiguration` del *Independent Test* de US2. | `ab88c4f` |
| C20 | BAJA | Corregido | Evidencia de SC-003 a nivel de proyecto (80,2 % solo unitarios frente a 98,6 % en SonarCloud, igual al JaCoCo agregado) registrada en T021; T021 y quickstart §3 quedan como verificación por archivo en `main` tras el merge. | `ab88c4f` |
| N07 | BAJA | Corregido | Nota de enmienda en research R5 que apunta a `TESTING.md`; el contrato nombra `SonarCloud Code Analysis` (12526); escenario 5 de US1: revisa el hotspot el dueño (FR-004a). | `b21cd82`, `ab88c4f` |
| N08 | BAJA | Corregido | Quitado "(review 003)" del comentario de `GlobalExceptionHandlerTest` (queda la razón técnica); plan.md: "ningún fichero bajo `src/main/` cambia". | `ab88c4f` |
| C10 | INFO | Diferido a la fila 010 | Fijar `actions/*` por SHA + Dependabot `github-actions`, marcado como prioritario por exponer `SONAR_TOKEN`. | `ab88c4f` (backlog) |
| C02 | BAJA | Diferido a la fila 010 | Test de paridad entre las listas de exclusión de Sonar y JaCoCo (tras C01 quedan 3 patrones Java + migraciones). | `ab88c4f` (backlog) |
| N05 | NIT | Diferido a la fila 010 | gitleaks descargado y ejecutado en `$RUNNER_TEMP`. | `ab88c4f` (backlog) |
| C17 | MEDIA | Diferido a un PR de seguimiento | Numerar y fechar `despliegue-azure-esqueleto`, replanificar 006–010 hacia el 1-3 dic y actualizar la fecha de demo en `CLAUDE.md` y la constitución: es replanificación del backlog, fuera del alcance de 003. | — |
| C04 | NIT | Se deja como está | `append=true` sobre un único `.exec`, documentado en `TESTING.md`. | — |
| C09 | — | Retirado por el revisor | Job `quality` con `sonar.qualitygate.wait=true`: no se publica. | — |
| C06, C07, C21 | Elogio | — | Sin cambios. | — |
| C23 | Plan | Hecho | Tras el push: pasar el PR a *ready* con Oscar como revisor, fusionar, aplicar el PUT y cerrar T021/T025/T032 y el `Status` de spec.md en un PR corto de cierre. | — |

### Aplicar el ruleset (C11, C12, N06): lo ejecutó el dueño el 2026-11-25 a las 22:00:14Z

Comprobado el 2026-11-27 en el PR de cierre de 003: `diff` vacío de `.rules`, `.enforcement` y `.conditions` entre el ruleset activo y el JSON.

Orden recomendado: fusionar el PR #3 y comprobar que `CODEOWNERS` de `main` es válido **antes** del PUT;
con un code owner inválido y `bypass_actors: []`, exigir su aprobación podría bloquear los merges.

```bash
gh api repos/JesusJs/the-tractor-store-backend-java/codeowners/errors --jq '.errors'
```

Esperado: `[]`. Después:

```bash
jq 'del(.id, .source_type, .source, .current_user_can_bypass, .node_id, ._links, .created_at, .updated_at)' docs/github/ruleset-main.json > /tmp/rs.json && gh api -X PUT repos/JesusJs/the-tractor-store-backend-java/rulesets/27647920 --input /tmp/rs.json
```

Verificación (ambos lados iguales, salida vacía de `diff`):

```bash
diff <(gh api repos/JesusJs/the-tractor-store-backend-java/rulesets/27647920 | jq -S '.rules') <(jq -S '.rules' docs/github/ruleset-main.json) && echo "ruleset activo = JSON"
```
