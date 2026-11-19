# Revisión 001 — 2026-11-19

Revisores: `spec-reviewer`, `architecture-reviewer` y `security-reviewer`. `./mvnw -q verify` terminó en **verde** antes de los cambios (EXIT=0): 22 tests, de ellos 6 unitarios y de arquitectura y 16 de integración, sin fallos. Cada hallazgo se ha comprobado contra el código. Los duplicados aparecen fusionados en una sola fila.

Después de las correcciones, `./mvnw clean verify` sigue en **verde**: 23 tests (7 unitarios y de arquitectura, 16 de integración).

| # | Severidad | Revisor | Archivo:línea | Hallazgo | Propuesta | Decisión |
|---|---|---|---|---|---|---|
| 1 | BLOQUEANTE | spec | `pr-evidence.md:244-249` | FR-018 / US5-5 no se cumplen. `gh api .../branches/main/protection` devuelve 404 *Branch not protected* y el documento solo describe el paso manual, sin evidencia. | El dueño del repo activa en `main` el ruleset con el check `build` obligatorio. Después se añade a `pr-evidence.md` la salida de `gh api` o una captura de *Merging is blocked*. | **Lo hace el dueño del repo** en GitHub: ruleset en `main` con el check `build` y PR obligatorio, sin aprobaciones. |
| 2 | ALTA | spec | `pr-evidence.md:239-242`, `tasks.md:165` (T040 [X]) | US5-4 / FR-015: el caso negativo del secreto solo se probó en local, pero T040 pide comprobarlo en el paso *Secret scan* del CI. | Rama desechable con una clave AWS de ejemplo no válida. Se enlaza el run en rojo (valor `REDACTED`) y se borra la rama. | **Aceptado** (ver justificación abajo). |
| 3 | MEDIA | spec | `pr-evidence.md:239` vs `:263` | La evidencia se contradice: la línea 239 dice que gitleaks se ejecutó en local y la 263 que no. | Dejar escrito lo que se hizo de verdad. | **Corregido**: el historial completo se escaneó en el CI; en local gitleaks solo se usó para la prueba negativa. Hay una nota de desviación en T039. |
| 4 | MEDIA | arquitectura, spec | `HexagonalArchitectureTests.java` | Con `allowEmptyShould(true)` y las capas sin clases, las reglas pasan sin comprobar nada. Que fallan ante violaciones solo se demostró a mano. | Test con *fixtures* que violan cada regla y esperan `AssertionError`. | **Diferido a 005** (`docs/backlog.md`). |
| 5 | MEDIA | arquitectura | `src/main/java/com/tractorstore/*/package-info.java` | `@ApplicationModule` no declaraba `allowedDependencies`, así que `catalog → cart.api` pasaba en verde. | Declarar `allowedDependencies` según la tabla de la skill `modular-architecture`. | **Corregido**: `catalog` = `{}` (el valor por defecto de Modulith 2 es `OPEN_TOKEN`, así que `{}` cierra el módulo), `inventory` → `order :: api`, `cart` → `catalog`/`inventory`/`order :: api`, `order` → `cart`/`catalog :: api`, `notifications` → `order :: api`. Las flechas a `order :: api` corresponden a los listeners de `OrderPlaced`. |
| 6 | MEDIA | arquitectura | `HexagonalArchitectureTests.java` | La regla de FR-005 no detectaba `JpaRepository` (`org.springframework.data.jpa.repository`) ni repositorios fuera de `persistence`. | `assignableTo(Repository.class)` y la regla «los `Repository` solo viven en `persistence`». | **Corregido**: `webDoesNotUsePersistence` usa `assignableTo(Repository.class)` y hay una regla nueva, `repositoriesLiveInPersistence`. |
| 7 | MEDIA | arquitectura | `HexagonalArchitectureTests.java` (regla ausente) | `xxx.api` puede exponer tipos de `domain`, `application` o `infrastructure` de su propio módulo. | Regla ArchUnit para `..api..`. | **Diferido a 005**. |
| 8 | MEDIA | seguridad | `.github/workflows/ci.yml:16,32` | Las actions se fijan por etiqueta movible, no por SHA. | Fijarlas por SHA y añadir Dependabot. | **Diferido a 010**. |
| 9 | MEDIA | seguridad | `.mvn/wrapper/maven-wrapper.properties:3` | Falta `distributionSha256Sum`. | Añadir el SHA-256 oficial. | **Diferido a 010**. |
| 10 | MEDIA | spec | `application.yml:16` | FR-013: `ddl-auto: validate` no tiene test. | Aserción sobre `spring.jpa.hibernate.ddl-auto`. | **Diferido a 002**. |
| 11 | BAJA | spec | `application-prod.yml:4-6` | FR-012 no tiene test. | IT con el perfil `prod` sin `DB_URL`. | **Diferido a 003**. |
| 12 | BAJA | spec | `contracts/operational-endpoints.md:11`, `ReadinessWhenDatabaseDownIT.java` | La respuesta DOWN del contrato no incluía `groups` y el test solo comprobaba el 503. | Corregir el contrato y comprobar el cuerpo. | **Corregido**: el contrato aplica la nota ¹ también a DOWN. El test comprueba `$.status == DOWN` y que no aparecen `components` ni `details`. |
| 13 | BAJA | arquitectura, spec | `plan.md`, `tasks.md` (T004, T005), `research.md` R8 | La documentación estaba desfasada respecto a `pom.xml` (gjf 1.36.0, `<skip>`). | Actualizar los textos. | **Corregido**. |
| 14 | BAJA | arquitectura | `pom.xml` (spotless) | `spotless:check` solo corría en `verify`, después de los IT. | Ligarlo a `validate`. | **Corregido**: `check` está en `validate`, así que `./mvnw test` ya falla con código sin formato. |
| 15 | BAJA | arquitectura | `HexagonalArchitectureTests.java` | Faltan reglas: `domain → application`, qué partes de Spring puede usar `application` y dónde puede ir `@Transactional`. | Añadirlas. | **Diferido a 005**. |
| 16 | BAJA | seguridad | `compose.yaml` | El puerto de la BD quedaba abierto en todas las interfaces. | `127.0.0.1:` delante del puerto. | **Corregido**. |
| 17 | BAJA | seguridad | `compose.yaml`, Testcontainers | `postgres:17` era una etiqueta flotante. | Fijar el parche en ambos sitios. | **Corregido**: `postgres:17.12` en `compose.yaml` y en la constante `TestcontainersConfiguration.POSTGRES_IMAGE`, que también usa `ReadinessWhenDatabaseDownIT`. |
| 18 | BAJA | seguridad | `.gitignore` | Solo se ignoraba `.env`. | `.env.*` y `!.env.example`. | **Corregido**. |
| 19 | BAJA | seguridad | `application.yml:21-27` | Swagger está activo en prod sin que la decisión esté escrita. | ADR y decidir después de la demo. | **Diferido a 010**. |
| 20 | BAJA | seguridad | `application-prod.yml:10` | `forward-headers-strategy` confía en los `X-Forwarded-*` de cualquiera. | Comprobar el ingress de ACA y anotarlo en el ADR 0005. | **Diferido a 003**. |

## Justificación del hallazgo 2 (aceptado)

La prueba negativa del secreto se hizo en local con **la misma versión de gitleaks (8.30.1) y los mismos flags** (`git --redact`) que el paso *Secret scan* del CI. Hacerla en el CI obligaría a empujar un secreto, aunque sea falso, a un repositorio público, donde queda en el historial y en los logs. No se suben secretos, ni falsos. El paso del CI está probado en positivo: escanea el historial completo y sale en verde.

## Evidencia de las correcciones 5 y 6

Se añadieron a mano cuatro clases temporales y después se borraron:
- `catalog.application` usando un record de `cart.api`.
- Un controller en `catalog.infrastructure.web` con un campo `JpaRepository`.
- Una interfaz `JpaRepository` en `catalog.application`.

`./mvnw test -Dtest='ModularityTests,HexagonalArchitectureTests'` falló con tres errores:
- `Module 'catalog' depends on named interface(s) 'cart :: api' ...`
- `webDoesNotUsePersistence`
- `repositoriesLiveInPersistence`

Sin esas clases vuelve a verde.

## Observación para 008

Con los listeners de `OrderPlaced` previstos, `cart → order :: api` y `order → cart :: api` forman un ciclo que `modules.verify()` rechazará cuando ambos tengan código. Hay que resolverlo en 008, por ejemplo moviendo el evento o leyendo el carrito por otra vía, y con una ADR si cambia alguna flecha.

## Descartados o sin acción en 001

- `ModularityTests` y «sin dependencias entre módulos» (US1-2): lo cubre ahora la corrección 5.
- `StartupFailFastIT` solo comprueba `ConnectException` y no el mensaje: aceptable.
- Regla de que `web` no devuelva `@Entity`: está aplazada a la 002 a propósito (plan.md:37, backlog).
