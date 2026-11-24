# Data Model: 003 — Calidad con SonarCloud

Esta funcionalidad **no añade tablas, entidades JPA ni migraciones**. Sus "entidades" son
configuración versionada (`pom.xml`, workflow, ruleset) y estado en servicios externos
(SonarCloud, GitHub). Aquí se fija qué datos tiene cada una, dónde vive y qué reglas cumple.

## Quality Gate

| Campo | Valor | Dónde vive |
|---|---|---|
| Nombre | `Sonar way` (integrado, no editable en el plan gratuito) | SonarCloud → proyecto |
| Cobertura código nuevo | ≥ 80 % | idem |
| Líneas duplicadas código nuevo | ≤ 3 % | idem |
| Rating fiabilidad / seguridad / mantenibilidad (nuevo) | A / A / A | idem |
| Security hotspots revisados (nuevo) | 100 % | idem |
| Regla de cambios pequeños | cobertura y duplicación no aplican con < 20 líneas nuevas | idem |

Estados: `PASSED` → check verde · `FAILED` → check rojo (FR-007). Sin análisis → no hay check →
merge bloqueado.

## Análisis

| Campo | Origen |
|---|---|
| Tipo | `pull_request` (PR contra `main`) o `branch` (`main`) |
| Disparador | evento `pull_request` o `push` a `refs/heads/main` (R4) |
| Código nuevo | PR: diff contra `main`; `main`: desde el primer análisis (R7) |
| Métricas | bugs, vulnerabilidades, code smells, hotspots, cobertura, duplicación |
| Resultado | estado del Quality Gate + resumen en el PR (app de GitHub) |

Precondición: todas las suites en verde en el mismo job (FR-009).

## Informe de cobertura agregado

| Campo | Valor |
|---|---|
| Datos brutos | `target/jacoco.exec` (surefire + failsafe, `append=true`) |
| Informe | `target/site/jacoco/jacoco.xml` (para Sonar) y `index.html` (local) |
| Generación | fase `verify` (`./mvnw clean verify` en local) |

## Exclusión

| Campo | Regla |
|---|---|
| Patrón | formato Sonar (`sonar.coverage.exclusions` / `sonar.cpd.exclusions`) y JaCoCo (`<excludes>`) |
| Ámbito | cobertura y/o duplicación |
| Justificación | obligatoria, en `TESTING.md` |

Lista cerrada en [research.md R5](research.md#r5-exclusiones-fr-010). Añadir un patrón fuera de
esa lista exige justificación escrita en `TESTING.md` y revisión en el PR.

## Ruleset de `main`

| Campo | Antes | Después |
|---|---|---|
| `required_status_checks` | `build` | `build` + check de la app de SonarCloud (nombre real, con `integration_id`) |
| Resto de reglas | sin cambios | sin cambios |

Fuente versionada: `docs/github/ruleset-main.json`, que MUST coincidir con el ruleset activo.

## Secreto

| Campo | Valor |
|---|---|
| Nombre | `SONAR_TOKEN` (ya creado como secreto del repositorio) |
| Uso | solo `env` del paso "Quality analysis" |
| Alcance | token de análisis del proyecto (mínimo posible, FR-015) |
| Rotación | documentada en `TESTING.md` sin valor |
