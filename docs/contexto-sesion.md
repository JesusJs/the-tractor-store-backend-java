# Contexto de sesión — handoff del backend

Estado a **2026-11-25**, para que cualquiera (el autor u otra persona) retome el proyecto sin historial previo.
Este documento resume y enlaza; la fuente de verdad sigue siendo la tabla "Fuente de verdad" de `CLAUDE.md`
(constitución, `docs/api-contract.md`, `docs/backlog.md`, `docs/adr/`, `specs/NNN-*/`).

## 1. Qué es

Backend Java 21 / Spring Boot 4.1 (monolito modular con Spring Modulith 2 y hexagonal por módulo) que cumple el
contrato del front Angular de micro-frontends del Tractor Store, **que no cambia**. Repo:
`JesusJs/the-tractor-store-backend-java`. Front: `JesusJs/the-tractore-store` (Nx 20, Module Federation).

## 2. Estado de cada spec

| Spec | Qué entrega | PR | Estado |
|---|---|---|---|
| 001 `esqueleto-modular` | Proyecto Boot 4.1 + Maven, 5 módulos con capas hexagonales, `Modulith.verify()` + ArchUnit, Spotless, compose con Postgres, Actuator, CI (build + gitleaks) | [#1](https://github.com/JesusJs/the-tractor-store-backend-java/pull/1) | Fusionada 2026-11-19 |
| 002 `catalogo` (fusiona persistencia, errores y catálogo) | Flyway V2/V3 con el dataset, JPA, Testcontainers, formato de error único + `traceId`, endpoints de catálogo según el front (ADR 0008), módulo técnico `shared` (ADR 0009), fixtures negativos de ArchUnit | [#2](https://github.com/JesusJs/the-tractor-store-backend-java/pull/2) | Fusionada 2026-11-24 |
| 003 `calidad-sonar` | JaCoCo (unit + IT, 80 % de líneas global), SonarCloud con Quality Gate Sonar way como check obligatorio, `TESTING.md`, Oscar como code owner (ADR 0010) | [#3](https://github.com/JesusJs/the-tractor-store-backend-java/pull/3) | Fusionada 2026-11-25 |
| 004 `inventario-pedidos` (fusiona inventario, carrito-sesión y pedidos; ADR 0011) | **P1** `GET /inventory/{sku}` y `GET /cart` (crea `ts_session`, carrito vacío: el carrito vive en `localStorage` del front). **P2** `POST /orders` y `GET /orders/{id}` con recibo (IVA 21 % `HALF_UP`), descuento de stock atómico con `UPDATE … WHERE stock >= :qty` y ligado a la sesión. Seed V4, esquema V5 | [#5](https://github.com/JesusJs/the-tractor-store-backend-java/pull/5) | **Abierto**, pendiente de revisión de Oscar y de los checks |

El PR #4 (`tmp/sc-002-quality-gate`) fue una prueba desechable del Quality Gate de la 003: está cerrado a propósito.

La revisión de la 004 y sus decisiones (corregido / diferido / aceptado) están en `specs/004-inventario-pedidos/review.md`.

## 3. Qué hay en `main`

Specs 001, 002 y 003 (último commit: merge del PR #3, `9439eea`). En `main` **no** están aún inventario, carrito ni
pedidos: llegan con el PR #5. Migraciones aplicadas en `main`: V1–V3; el PR #5 añade V4 (`inventory_stock`) y V5
(`order_*`). Las migraciones aplicadas no se editan nunca: siempre una nueva.

## 4. Pendientes priorizados

1. **Fusionar el PR #5**: aprobación de Oscar, hilos resueltos, checks `build` y `SonarCloud Code Analysis` en verde.
2. **Despliegue en Azure** (fila `despliegue-azure-esqueleto` del backlog; imprescindible para una demo con el front
   desplegado): Dockerfile multi-stage no-root, Terraform (RG, ACR, Container Apps, PostgreSQL Flexible, Log
   Analytics), GitHub Actions con OIDC, `/actuator/health` público, IT del perfil `prod` sin `DB_URL`.
3. **Issues del front** (repo del front; no hay issues abiertos en GitHub, verificado en el código):
   - El botón "Add to cart" sigue activo con "Out of stock": en
     `packages/mfe-decide/src/app/remote-entry/entry.component.html` el `[disabled]` solo mira
     `isAddingToCart() || !selectedVariant()`, no `isOutOfStock`.
   - Ante un 409 (`OUT_OF_STOCK`) el usuario ve un error genérico: `http-error.interceptor.ts` convierte cualquier
     error HTTP en `Server error code …` y descarta el código del contrato.
   - Sonar con cobertura en el front: el repo del front no tiene configuración de Sonar.
4. **P3 de la 004** (fila "Continuación de 004\*" del backlog, tras la demo): fases 5–7 de
   `specs/004-inventario-pedidos/tasks.md`: guardas ArchUnit FR-028..FR-030 (T055–T061), evento `OrderPlaced` con
   outbox (`spring-modulith-starter-jpa`, migración V6 del Event Publication Registry) y listener en `notifications`
   (T062–T073), pulido (T074–T078).
5. **Fila 010 `entrega-final`**: CD con smoke test, logs JSON en prod, README/DEPLOYMENT.md, `actions/*` por SHA +
   Dependabot, `SONAR_TOKEN` a *Scoped Organization Token*, test de `Secure` en `ts_session` con `prod`, y lo diferido
   de la review 004: límite de frecuencia de `POST /orders`, Σ cantidad por SKU, prefijo `__Host-` y límite de tamaño
   del cuerpo.

Orden de recorte si falta tiempo (backlog): 009 (dejar un E2E), P3 de la 004, 010 (CD manual).

## 5. Arrancar en local

Requisitos: Java 21, Docker, `psql` (para el script de reset), `jq`/`curl` para probar; `pnpm` para el front.

```bash
docker compose up -d --wait postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev     # escucha en http://localhost:5271
```

- **Puerto 5432 ocupado** por un PostgreSQL nativo del sistema: no lo pares; usa otro puerto en compose y en la app
  (ambos leen `DB_PORT`):
  ```bash
  DB_PORT=55432 docker compose up -d --wait postgres
  DB_PORT=55432 ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
  ```
  Los IT no se ven afectados: Testcontainers levanta su propio PostgreSQL.
- **Permisos de Docker**: si el usuario se acaba de añadir al grupo `docker` y la sesión aún no lo tiene, ejecuta el
  comando dentro del grupo sin cerrar sesión: `sg docker -c "./mvnw verify"`.
- **Docker Engine nativo vs Docker Desktop**: en Linux son dos daemons distintos, con contexto y socket propios
  (`docker context ls`). Testcontainers usa por defecto `/var/run/docker.sock` (Engine nativo). Si solo corre Docker
  Desktop (`systemctl --user start docker-desktop`), hay que apuntar `DOCKER_HOST` a su socket o usar el Engine
  nativo; comprueba con `docker info` qué daemon responde antes de culpar a los tests.
- **Restaurar el stock de la demo** (tras pedidos de prueba o antes de presentar): `scripts/reset-demo-stock.sh`
  (por defecto `127.0.0.1:5432`, como `compose.yaml`; `DB_PORT=55432 scripts/reset-demo-stock.sh` si publicaste
  Postgres en otro puerto). Reaplica el `INSERT` de V4, conserva los pedidos y se niega a tocar hosts no locales;
  sin `psql` en el equipo usa el del contenedor (`docker compose exec -T postgres psql`).
- **Comprobar**: `curl -s localhost:5271/api/v1/inventory/CL-01-GR | jq` (stock 10; `CL-01-GY` → 0), Swagger UI en
  `http://localhost:5271/swagger-ui.html`, recorrido completo en `specs/004-inventario-pedidos/quickstart.md`.
- **Front** (en su repo): `NX_DAEMON=false pnpm nx run-many -t serve --projects="mfe_explore,mfe_decide,mfe_checkout"`
  y, en otra terminal, `NX_DAEMON=false pnpm nx serve shell --skipRemotes=mfe_explore,mfe_decide,mfe_checkout`
  (puertos 4200–4203). `--devRemotes` deja el shell en blanco.

Tests: `./mvnw test` (unit + arquitectura), `./mvnw verify` (+ integración; necesita Docker), `./mvnw spotless:apply`
(formato). Detalle en `TESTING.md`.

## 6. Ciclo de trabajo con Spec Kit

Una funcionalidad = una spec = una rama `NNN-nombre` = un PR.

```
/speckit-specify → /speckit-clarify → (aprobación) → /clear
/speckit-plan → /speckit-tasks → /speckit-analyze → (aprobación) → /clear
/speckit-implement   (dominio → aplicación → adaptadores; prueba primero)
/review-spec         (subagentes spec, arquitectura y seguridad) → corregir, diferir o aceptar cada hallazgo
/pr → /log-practica
```

- Cambios de principio o de arquitectura → `/adr` antes de tocar código.
- El contrato lo manda el front real: leer sus modelos y servicios antes de especificar (ADR 0008 y 0011).
- Antes de configurar Boot 4 / Jackson 3, cargar la skill `spring-boot-4-notes` y verificar nombres en los
  metadatos de la versión exacta.
- Bitácora de prácticas y aprendizajes: `docs/practicas-ia.md` (falta la fila de la 003).

## 7. Reglas del repo

- **Ruleset `main-protection`** sobre la rama por defecto (copia en `docs/github/ruleset-main.json`):
  - PR obligatorio con **1 aprobación de code owner**; Oscar (`oscarmoraquind`) es el único code owner
    (`.github/CODEOWNERS`).
  - **Dismiss stale reviews on push**: cualquier push tras la aprobación la anula y hay que pedirla de nuevo.
  - **Todos los hilos de revisión resueltos** antes de fusionar.
  - Checks obligatorios y rama al día con `main`: `build` (GitHub Actions) y `SonarCloud Code Analysis`.
  - Sin borrado de la rama ni force-push.
- Antes de cualquier comando de infraestructura o de GitHub que lea archivos del repo (p. ej. un `PUT` del ruleset),
  comprobar la rama con `git branch --show-current`: el archivo que se aplica es el de la rama actual.
- Conventional commits en inglés, sin `Co-Authored-By` ni firmas de herramientas; documentación en español.
- **Un comando de barra por mensaje** al trabajar con Claude Code (`/speckit-*`, `/review-spec`, `/pr`,
  `/log-practica`…): encadenar varios en un mismo mensaje mezcla sus pasos.
- Sin secretos en el repo: configuración por variables de entorno; `SONAR_TOKEN` solo como secreto de Actions;
  gitleaks corre en CI.
