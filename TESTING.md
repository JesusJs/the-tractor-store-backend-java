# Pruebas y calidad

## Tipos de test y comandos

| Tipo | Nombre | Plugin | Comando |
|---|---|---|---|
| Unitarios + arquitectura (`ModularityTests`, ArchUnit) | `*Test`, `*Tests` | surefire | `./mvnw test` |
| Integración con PostgreSQL real (Testcontainers, nunca H2) | `*IT` | failsafe | `./mvnw verify` (necesita Docker) |

Cobertura agregada en local, sin SonarCloud: `./mvnw clean verify` → `target/site/jacoco/index.html`.
Surefire y failsafe escriben en un único `target/jacoco.exec` (`append=true`), así que el informe suma
unitarios e integración. Si configuras `argLine` en surefire/failsafe, empieza por `@{argLine}` o se
pierde el agente de JaCoCo.

## Exclusiones de cobertura

Definidas en `pom.xml` (`sonar.coverage.exclusions` y `<excludes>` de JaCoCo). Ninguna toca `domain/`,
`application/` ni los adaptadores.

| Qué | Patrón (Sonar) | Por qué |
|---|---|---|
| DTOs HTTP | `**/infrastructure/web/dto/**` | records sin lógica |
| API entre módulos | `**/api/**` | contratos: records, interfaces, eventos |
| Cuerpo de error | `**/shared/infrastructure/web/ApiError.java` | record sin lógica |
| Configuración | `**/infrastructure/config/**`, `**/infrastructure/**/*Configuration.java` | cableado de Spring; lo ejercitan los `*IT` |
| Arranque | `**/TractorStoreApplication.java` | solo `main` |
| Migraciones | `src/main/resources/db/migration/**` (también fuera de duplicación) | SQL repetitivo por naturaleza |

`CorsProperties` y las entidades JPA **no** se excluyen. Una exclusión nueva exige justificación en esta
tabla y revisión en el PR.

## Quality Gate (Sonar way, sobre código nuevo)

Cobertura ≥ 80 %, duplicación ≤ 3 %, rating A en fiabilidad, seguridad y mantenibilidad (0 bugs y 0
vulnerabilidades nuevos) y 100 % de hotspots revisados. El plan gratuito no permite el 85 % pedido
([ADR 0010](docs/adr/0010-sonarcloud-en-alcance-sonar-way.md)).

- **Regla de < 20 líneas**: si el código nuevo tiene menos de 20 líneas, cobertura y duplicación no se
  evalúan. Un PR solo de documentación o de ficheros excluidos pasa.
- **Código nuevo**: en un PR, su diff contra `main`. En `main` ("Previous version", versión Maven fija),
  todo lo añadido desde su primer análisis; el estado de `main` es informativo, lo que bloquea es el PR.
- El paso **Quality analysis** del job `build` solo corre si todas las suites pasan. El check
  obligatorio es `SonarCloud Code Analysis` ([ruleset](docs/github/ruleset-main.json)).

## Revisar un PR

La app de SonarCloud deja en la conversación del PR un resumen con el estado del gate y las métricas de
código nuevo; su enlace abre el detalle (issues con fichero y línea) sin cuenta, porque el proyecto es
público. Solo `JesusJs` marca un hotspot como *Safe* o un issue como *False Positive*/*Won't Fix*, y por
cada marca comenta en el PR con enlace y justificación, que Oscar/Steven validan antes de aprobar. No se
desactivan reglas globalmente sin documentarlo aquí.

## Operación de `SONAR_TOKEN`

- Rotación: SonarCloud → *My Account → Security* → token de análisis del proyecto con caducidad (anota la
  fecha); después `gh secret set SONAR_TOKEN` (pide el valor por consola, nunca en la línea de comandos)
  y revoca el anterior. Nunca se escribe el valor en el repo, en `-D` ni en logs.
- Token caducado, inválido o ausente → **Quality analysis** rojo (403) y merge bloqueado: rótalo.
- SonarCloud caído → el dueño quita temporalmente el check del ruleset, comenta la causa en el PR, fusiona
  y lo restaura (`docs/github/ruleset-main.json` sigue reflejando el estado normal).
- PRs desde forks no reciben el secreto y quedan bloqueados (aceptado: no hay contribuciones externas).
