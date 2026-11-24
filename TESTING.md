# Pruebas y calidad

## Tipos de test y comandos

| Tipo | Nombre | Plugin | Comando |
|---|---|---|---|
| Unitarios + arquitectura (`ModularityTests`, ArchUnit) | `*Test`, `*Tests` | surefire | `./mvnw test` |
| Integración con PostgreSQL real (Testcontainers, nunca H2) | `*IT` | failsafe | `./mvnw verify` (necesita Docker) |

Cobertura agregada en local, sin SonarCloud: `./mvnw clean verify` → `target/site/jacoco/index.html`.
Surefire y failsafe escriben en un único `target/jacoco.exec` (`append=true`), así que el informe suma
unitarios e integración. Si configuras `argLine` en surefire/failsafe, empieza por `@{argLine}` o se
pierde el agente de JaCoCo. Como `append=true` suma también las ejecuciones locales anteriores,
haz siempre `clean` antes de mirar el informe: sin él, datos de clases ya cambiadas inflan o falsean la
cobertura (JaCoCo avisa de *execution data mismatch*). En CI no pasa porque el runner empieza limpio.

## Exclusiones de cobertura

Definidas en `pom.xml` (`sonar.coverage.exclusions` y `<excludes>` de JaCoCo); las dos listas se
mantienen sincronizadas. Ninguna toca `domain/`, `application/` ni los adaptadores con lógica.

| Qué | Patrón Sonar | Patrón JaCoCo | Por qué |
|---|---|---|---|
| DTOs HTTP | `**/infrastructure/web/dto/**` | `**/infrastructure/web/dto/**` | records sin lógica |
| API entre módulos | `src/main/java/com/tractorstore/*/api/**` | `com/tractorstore/*/api/**` | contratos sin lógica: interfaces, records, eventos |
| Cuerpo de error | `**/shared/infrastructure/web/ApiError.java` | `**/shared/infrastructure/web/ApiError.class` | record sin lógica |
| Configuración | `**/infrastructure/config/**` | `**/infrastructure/config/**` | cableado de Spring; lo ejercitan los `*IT` |
| Arranque | `**/TractorStoreApplication.java` | `**/TractorStoreApplication.class` | solo `main` |
| Migraciones | `src/main/resources/db/migration/**` (también fuera de duplicación) | n/a (JaCoCo no mide SQL) | SQL repetitivo por naturaleza |

`CorsConfiguration`, `CorsProperties` y las entidades JPA **no** se excluyen: la configuración de CORS
tiene lógica de seguridad y la cubre `CorsIT`. Una exclusión nueva exige justificación en esta tabla y
revisión en el PR.

**Regla del paquete `api`**: la exclusión solo vale para el `api` de primer nivel de cada módulo
(`com.tractorstore.<módulo>.api`) y mientras contenga contratos sin lógica. Si un record o evento de `api`
gana lógica (validación en un constructor compacto, métodos de fábrica), esa clase se mueve fuera de
`api` o se saca de la exclusión en el mismo PR.

## Quality Gate (Sonar way, sobre código nuevo)

Cobertura ≥ 80 %, duplicación ≤ 3 %, rating A en fiabilidad, seguridad y mantenibilidad (0 bugs y 0
vulnerabilidades nuevos) y 100 % de hotspots revisados. El plan gratuito no permite el 85 % pedido
([ADR 0010](docs/adr/0010-sonarcloud-en-alcance-sonar-way.md)).

- **Regla de < 20 líneas**: si el código nuevo tiene menos de 20 líneas, cobertura y duplicación no se
  evalúan. Un PR solo de documentación o de ficheros excluidos pasa.
- El paso **Quality analysis** del job `build` solo corre si todas las suites pasan. El check
  obligatorio es `SonarCloud Code Analysis` ([ruleset](docs/github/ruleset-main.json)).

## Código nuevo y línea base

- En un PR, código nuevo es su diff contra `main`; eso es lo que bloquea el merge.
- En `main`, *New Code* = "Previous version". Como la versión Maven no cambia (`0.0.1-SNAPSHOT`), la línea
  base es el primer análisis de `main`: el código de 001 y 002 no tiene que llegar al 80 %. El estado de
  `main` es informativo.
- Cómo comprobarlo: SonarCloud → proyecto → *Administration → New Code* (o *Project Settings → New
  Code*) debe mostrar "Previous version".

## Revisar un PR

La app de SonarCloud deja en la conversación del PR un resumen con el estado del gate y las métricas de
código nuevo; su enlace abre el detalle (issues con fichero y línea) sin cuenta, porque el proyecto es
público. Solo `JesusJs` marca un hotspot como *Safe* o un issue como *False Positive*/*Won't Fix*, y por
cada marca comenta en el PR con enlace y justificación, que Oscar/Steven validan antes de aprobar. No se
desactivan reglas globalmente sin documentarlo aquí.

## Operación de `SONAR_TOKEN`

- Caducidad del token actual: **diciembre de 2026**. Rótalo antes de esa fecha.
- Rotación: SonarCloud → *My Account → Security* → token de análisis del proyecto con caducidad (anota la
  fecha aquí); después `gh secret set SONAR_TOKEN` (pide el valor por consola, nunca en la línea de comandos)
  y revoca el anterior. Nunca se escribe el valor en el repo, en `-D` ni en logs.
- Token caducado, inválido o ausente → **Quality analysis** rojo (403) y merge bloqueado: rótalo.
- SonarCloud caído → el dueño quita temporalmente el check del ruleset, comenta la causa en el PR, fusiona
  y lo restaura (`docs/github/ruleset-main.json` sigue reflejando el estado normal). Después comprueba que
  el ruleset activo vuelve a coincidir con el JSON:
  `gh api repos/JesusJs/the-tractor-store-backend-java/rulesets/<id> --jq '.rules'` frente a
  `jq '.rules' docs/github/ruleset-main.json` (el `<id>` sale de `gh api repos/JesusJs/the-tractor-store-backend-java/rulesets`).
- PRs desde forks no reciben el secreto y quedan bloqueados (aceptado: no hay contribuciones externas).
