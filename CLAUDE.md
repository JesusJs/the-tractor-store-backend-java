# Tractor Store — Backend (Java)

Backend del Tractor Store (reto de micro-frontends). El front Angular ya existe y **no cambia**: este backend debe cumplir su contrato.
Demo y cierre: **3 de diciembre de 2026** (fechas de cada spec en `docs/backlog.md`).

## Stack
- Java 21 · Spring Boot 4.1.x · Maven (wrapper `./mvnw`) · Spring Modulith 2.x
- PostgreSQL + Flyway (sin `ddl-auto=update`; en todos los perfiles `validate`)
- JUnit + AssertJ + Mockito · Testcontainers (PostgreSQL real, nunca H2) · ArchUnit
- springdoc-openapi (Swagger UI) · Actuator
- JaCoCo · SonarCloud con Quality Gate Sonar way (obligatorio para fusionar; ADR 0010, detalle en `TESTING.md`)
- Docker multi-stage · Azure Container Apps + Azure Database for PostgreSQL · Terraform · GitHub Actions con OIDC

> Spring Boot 4 tiene diferencias importantes con Boot 3. Antes de escribir configuración, tests o serialización, carga la skill `spring-boot-4-notes`.

## Fuente de verdad (léela antes de proponer nada)
| Qué | Dónde |
|---|---|
| Principios no negociables | `.specify/memory/constitution.md` |
| Contrato de API (9 endpoints, error único) | `docs/api-contract.md` (skill `api-contract`) |
| Requisitos derivados de la guía | `docs/requisitos.md` |
| Backlog y orden de specs | `docs/backlog.md` |
| Decisiones | `docs/adr/` |
| Specs por funcionalidad | `specs/NNN-nombre/` (generadas por Spec Kit) |

## Arquitectura (resumen; detalle en skills `modular-architecture` y `hexagonal-architecture`)
- Monolito modular. Módulos de dominio: `catalog`, `inventory`, `cart`, `order`, `notifications`; más el módulo técnico abierto `shared` (jerarquía de errores, `traceId`, CORS y la sesión por cookie `ts_session`; sin entidades ni tablas, ADR 0009 y 0011).
- Paquete raíz `com.tractorstore`. Dentro de cada módulo:
  - `api/` → única cara pública (interfaz `XxxApi`, records de intercambio, eventos). Marcado con `@NamedInterface("api")`.
  - `domain/` → modelo y puertos de salida. **Java puro, cero imports de Spring/JPA/Jackson.**
  - `application/` → casos de uso (puertos de entrada) e implementación.
  - `infrastructure/` → adaptadores: `web/` (controllers + DTOs HTTP), `persistence/` (entidades JPA + adaptadores), `config/`.
- Los controllers **solo** llaman casos de uso (nunca repositorios).
- Un módulo solo usa de otro su paquete `api` o sus eventos.
- Tablas con prefijo de módulo: `catalog_`, `inventory_`, `cart_`, `order_`. Sin JOINs entre módulos.

## Flujo de trabajo con IA (obligatorio)
Una funcionalidad = una spec = una rama `NNN-nombre` = un PR.

```
/speckit-specify  → /speckit-clarify → (yo apruebo) → /clear
/speckit-plan     → /speckit-tasks   → /speckit-analyze → (yo apruebo) → /clear
/speckit-implement   (de adentro hacia afuera: dominio → aplicación → adaptadores; prueba primero)
/review-spec      → corrige hallazgos
/pr               → /log-practica
```
- Prompts largos en `docs/prompts/`; se invocan con `@docs/prompts/archivo.md`.
- Cambios de principio o de arquitectura → `/adr` antes de tocar código.

## Comandos
```bash
docker compose up -d postgres     # BD local
./mvnw spotless:apply             # formato
./mvnw test                       # unit + arquitectura
./mvnw verify                     # + integración (Testcontainers; necesita Docker)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Reglas para ti, Claude
- Respuestas y documentación en **español**; código, nombres de clases y commits en inglés (conventional commits).
- Commits sin líneas `Co-Authored-By` ni de atribución.
- No inventes campos del contrato: si falta un dato, pregúntame o márcalo `[NEEDS CLARIFICATION]`.
- Nunca leas ni escribas secretos; usa variables de entorno.
- Cuando termines un paso, di qué se puede **ver** (comando, URL o test) para comprobarlo.
- Explícame en 2-3 líneas el concepto nuevo que aplicaste (estoy aprendiendo el stack).

## Reglas de trabajo (salvaguardas de las reviews de los PR #3 y #5)
- Todo lo diferido lleva fila en `docs/backlog.md` con número de spec y fecha, y nunca contradice una regla de su propio `tasks.md`.
- Desviarse de la guía o de `docs/requisitos.md` exige una decisión del mentor registrada en `docs/requisitos.md`.
- Antes de cada commit: `git diff --cached --stat` y comprobar que el contenido coincide con el mensaje.
- Los scripts de edición (bash, python por `heredoc`) empiezan con `set -euo pipefail` o fallan si un reemplazo no encuentra el texto.
- Antes de cualquier cambio de infraestructura (ruleset, secretos, Terraform, Azure): `git branch --show-current`.
- Compromisos con el mentor con fecha → issue con la etiqueta `mentor-condition` (plantilla en `.github/ISSUE_TEMPLATE/mentor-condition.md`).
