# Contrato: pipeline de calidad y checks de `main`

Esta spec no toca la API HTTP (`docs/api-contract.md` sin cambios). Su interfaz externa es el
pipeline: qué dispara el análisis, qué checks ve un PR y qué exige GitHub para fusionar.

## Disparadores

| Evento | ¿Corre `build`? | ¿Corre "Quality analysis"? | Tipo de análisis |
|---|---|---|---|
| `pull_request` contra `main` | sí | sí | PR (código nuevo = diff contra `main`) |
| `push` a `main` | sí | sí | rama `main` |
| `push` a otra rama | sí | no | — (el PR ya lo analiza) |
| PR desde fork | sí | falla por falta de secreto | — (aceptado, sin merge) |

## Pasos del job `build` (orden obligatorio, FR-016)

1. Checkout (`fetch-depth: 0`)
2. Secret scan (gitleaks)
3. Set up Java 21
4. Format (`spotless:check`)
5. Unit + architecture (`./mvnw -B test`) → escribe `target/jacoco.exec`
6. Integration (`./mvnw -B verify -DskipUnitTests …`) → añade a `jacoco.exec` y genera
   `target/site/jacoco/jacoco.xml`
7. Package
8. **Quality analysis** (nuevo): `./mvnw -B sonar:sonar`, `env: SONAR_TOKEN`, condición de R4

Si cualquier paso 1–7 falla, el 8 no se ejecuta (FR-009).

## Checks de un PR

| Check | Publicado por | Verde cuando | Obligatorio |
|---|---|---|---|
| `build` | GitHub Actions (este workflow) | pasos 1–8 sin error | sí (ya lo era) |
| Check de SonarCloud (nombre exacto según el primer PR; R6) | app de GitHub de SonarCloud | Quality Gate Sonar way `PASSED` | **sí (nuevo)** |

Además, la app publica en la conversación del PR el resumen del Quality Gate con enlace al
detalle (FR-013), visible sin cuenta porque el proyecto es público.

## Propiedades del análisis (`pom.xml`)

| Propiedad | Valor |
|---|---|
| `sonar.organization` | `jesusjs` |
| `sonar.projectKey` | `JesusJs_the-tractor-store-backend-java` |
| `sonar.host.url` | `https://sonarcloud.io` |
| `sonar.coverage.jacoco.xmlReportPaths` | `target/site/jacoco/jacoco.xml` |
| `sonar.coverage.exclusions` | lista de [research R5](../research.md#r5-exclusiones-fr-010) |
| `sonar.cpd.exclusions` | `src/main/resources/db/migration/**` |
| Token | **nunca** en el pom: variable de entorno `SONAR_TOKEN` |

## Garantías

- Fallo del Quality Gate → check de Sonar **rojo** (no neutral/skipped) → merge bloqueado.
- Análisis ausente (token inválido, servicio caído, fork) → falta un check obligatorio → merge
  bloqueado.
- El valor del token no aparece en el repo ni en logs (enmascarado por GitHub; gitleaks sigue
  en verde).
