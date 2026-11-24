<!--
Sync Impact Report
- Cambio de versión: 1.1.0 → 1.2.0 (MINOR: se amplía materialmente el alcance del principio VII y
  la condición de merge del flujo; no se elimina ni redefine de forma incompatible ningún principio)
- Principios modificados:
  VII. Simplicidad y plazo (mismo título): SonarCloud sale de "Fuera de alcance" y entra en alcance
  solo para el backend, con el Quality Gate integrado Sonar way (cobertura de código nuevo ≥ 80 %,
  limitación del plan gratuito). Referencia añadida: ADR 0010.
- Secciones modificadas: Restricciones técnicas (+ JaCoCo y SonarCloud); Flujo de desarrollo
  (merge exige además el Quality Gate de SonarCloud aprobado).
- Secciones añadidas: ninguna
- Secciones eliminadas: ninguna
- Plantillas dependientes: no se modifican (leen la constitución en tiempo de ejecución).
  ✅ docs/backlog.md y docs/requisitos.md actualizados en el mismo cambio (spec 003, FR-001).
  ⚠ Pendiente en la implementación de 003: skill `testing-strategy` (80 % código nuevo, FR-012).
- TODOs diferidos: ninguno.
- Aprobación: usuario, 2026-11-23 (spec 003-calidad-sonar, ADR 0010).
-->

# Constitución de Tractor Store — Backend Java

## Principios fundamentales

### I. Contrato primero

`docs/api-contract.md` es la fuente de verdad del contrato con el front Angular, que no cambia.

- Toda ruta MUST usar el prefijo `/api/v1/<módulo>`.
- Todo error MUST responder con el formato único `{ code, message, details?, traceId }`;
  `traceId` MUST viajar también en la cabecera `X-Trace-Id`.
- Ningún endpoint, campo o código de error fuera del contrato sin una ADR previa que lo añada.
- Las entidades JPA MUST NOT aparecer en ninguna respuesta HTTP: los controllers devuelven DTOs
  (records) con mapeo explícito.
- Si falta un dato del contrato, se marca `[NEEDS CLARIFICATION]`; nunca se inventa.

**Cómo se comprueba**: tests `@WebMvcTest` por endpoint que verifican ruta, código HTTP y forma
del JSON (incluido el de error); test ArchUnit que prohíbe que los paquetes `infrastructure.web`
devuelvan tipos anotados con `@Entity`; revisión del subagente `spec-reviewer` contra el contrato.

**Razón**: el front ya existe; cualquier desviación rompe la demo.

### II. Monolito modular + hexagonal

- Un único despliegue con cinco módulos de dominio (`catalog`, `inventory`, `cart`, `order` y
  `notifications`) y un módulo técnico `shared`, todos bajo `com.tractorstore`.
- `shared` MUST limitarse a lo técnico y transversal (jerarquía de errores, `traceId`, CORS):
  se declara `@ApplicationModule(type = OPEN)`, no depende de ningún otro módulo y no contiene
  lógica de negocio, entidades ni tablas. Añadirle otra cosa requiere ADR. Su paquete
  `shared.error` MUST ser Java puro, porque lo usan los `domain`.
- Cada módulo MUST organizarse en `api` (única cara pública, `@NamedInterface("api")`), `domain`,
  `application` e `infrastructure` (`web`, `persistence`, `config`).
- `domain` MUST ser Java puro: cero imports de Spring, JPA (`jakarta.persistence`) o Jackson.
- Los controllers MUST llamar solo a casos de uso (puertos de entrada), nunca a repositorios.
- Un módulo MUST usar de otro únicamente su paquete `api` o sus eventos.

**Cómo se comprueba**: `ApplicationModules.of(...).verify()` (`ModularityTests`) y reglas ArchUnit
(`HexagonalArchitectureTests`) se ejecutan en `./mvnw test` en cada build local y de CI; un fallo
bloquea el merge. Referencias: ADR 0001 y ADR 0009 (módulo `shared`).

**Razón**: fronteras explícitas y testeables; el backend anterior falló por no tenerlas.

### III. Pruebas primero, de adentro hacia afuera (NO NEGOCIABLE)

- Cada tarea de implementación MUST empezar con un test que falla (rojo) antes del código que lo
  hace pasar; el orden es dominio → aplicación → adaptadores.
- Dominio: tests unitarios sin contexto Spring.
- Adaptadores: test slices (`@WebMvcTest`, `@DataJpaTest`).
- Flujos completos: Testcontainers con PostgreSQL real. H2 u otra base en memoria está prohibida.
- Cada bug del backend anterior listado en `docs/backlog.md` MUST tener un test de regresión en
  la spec que lo cubre.

**Cómo se comprueba**: `tasks.md` coloca cada test antes de su implementación; `/speckit-analyze`
lo verifica; `./mvnw verify` en CI ejecuta la pirámide completa; búsqueda de `h2` en `pom.xml`
debe dar cero resultados.

**Razón**: el plazo es corto y no hay QA; los tests son la única red de seguridad.

### IV. Datos

- Cada módulo MUST ser dueño exclusivo de sus tablas, con prefijo `catalog_`, `inventory_`,
  `cart_`, `order_`. No hay JOINs ni claves foráneas entre tablas de módulos distintos.
- Esquema y dataset MUST gestionarse solo con migraciones Flyway versionadas; una migración
  aplicada no se edita. `spring.jpa.hibernate.ddl-auto=validate` en todos los perfiles.
- El dinero MUST ser `NUMERIC(10,2)` en BD y `BigDecimal` en Java (nunca `double`/`float`).
- El precio MUST salir siempre del catálogo; el cliente nunca envía precios.
- Open Session In View MUST estar desactivado (`spring.jpa.open-in-view=false`).

**Cómo se comprueba**: los tests de integración arrancan sobre un PostgreSQL vacío y migran;
revisión de `application*.yml`; test de regresión "precio enviado por el cliente" (spec 007).
Referencia: ADR 0003.

**Razón**: BD reproducible en local, Testcontainers y Azure; sin acoplamiento por datos.

### V. Eventos internos

- Los eventos MUST nombrarse en pasado (`OrderPlaced`) y ser records en el paquete `api` del
  módulo que los publica.
- Los consumidores MUST usar `@ApplicationModuleListener`.
- La entrega MUST apoyarse en el Event Publication Registry de Spring Modulith (patrón outbox),
  con su tabla creada por migración Flyway.
- Sin brokers externos (Kafka, RabbitMQ, etc.).

**Cómo se comprueba**: tests con `Scenario` de Spring Modulith para cada evento y un test de
reintento con un listener que falla una vez. Referencia: ADR 0002.

**Razón**: desacopla `order` de `cart`, `inventory` y `notifications` sin coste operativo.

### VI. Seguridad proporcionada

- Sin login, JWT ni Keycloak.
- Carrito y pedidos MUST ligarse a la cookie `ts_session` (UUID aleatorio, HttpOnly,
  `SameSite=Lax`, `Secure` en prod). Consultar un pedido de otra sesión MUST devolver 404.
- Toda entrada (body, path, query) MUST validarse con Bean Validation.
- CORS MUST declarar orígenes explícitos; nunca `*` con credenciales.
- Cero secretos en el repositorio o en la imagen Docker: solo variables de entorno.

**Cómo se comprueba**: test de integración "pedido de otra sesión → 404"; tests de validación
(400 `VALIDATION_ERROR`); escaneo de secretos en CI; revisión del subagente `security-reviewer`.
Referencia: ADR 0004.

**Razón**: seguridad acorde a una demo sin usuarios, sin dejar huecos obvios.

### VII. Simplicidad y plazo

- La demo es el 2026-11-25; YAGNI manda.
- Fuera de alcance: Keycloak, Grafana, OpenTelemetry completo, caché, k6, Kafka.
- En alcance desde la spec 003: SonarCloud, solo para este backend (ni front ni instalación
  autoalojada), con el Quality Gate integrado **Sonar way** (código nuevo: cobertura ≥ 80 %,
  duplicación ≤ 3 %, ratings A, 100 % de hotspots revisados). El 80 % es una limitación del plan
  gratuito, que no admite Quality Gates propios; cambiar de Quality Gate o de plan requiere ADR.
- Cualquier complejidad no exigida por la spec MUST justificarse en la sección
  "Complexity Tracking" del `plan.md`.

**Cómo se comprueba**: `/speckit-analyze` y la "Constitution Check" del plan señalan
dependencias o componentes fuera de alcance; el subagente `architecture-reviewer` lo revisa.
Referencia: ADR 0010 (SonarCloud en alcance).

**Razón**: una sola persona y nueve días; cada pieza extra compite con la demo.

### VIII. Entregable y operable

- Imagen Docker multi-stage que MUST ejecutarse con usuario no-root.
- Actuator `health` (liveness/readiness) MUST estar expuesto para las probes.
- Despliegue a Azure Container Apps con Terraform y GitHub Actions autenticado por OIDC
  (sin secretos de larga duración).
- Configuración por perfiles (`dev`, `test`, `prod`) y variables de entorno.

**Cómo se comprueba**: `docker run` + `docker inspect` muestran usuario no-root; smoke test
`GET /actuator/health` tras cada deploy; `terraform plan` sin cambios inesperados.
Referencia: ADR 0005.

**Razón**: el despliegue en Azure es parte del objetivo de aprendizaje y de la demo.

## Restricciones técnicas

- Java 21 · Spring Boot 4.1.x · Spring Modulith 2.x · Maven wrapper (`./mvnw`).
- PostgreSQL + Flyway · springdoc-openapi (Swagger UI) · Actuator.
- Calidad: JaCoCo (cobertura agregada de unitarios, arquitectura e integración en un único
  informe) · SonarCloud (plan gratuito, Quality Gate Sonar way; token solo como secreto de
  GitHub). Ambos plugins de build con versión exacta (ADR 0010).
- JUnit + AssertJ + Mockito · Testcontainers · ArchUnit.
- Formato con Spotless (google-java-format); `./mvnw spotless:check` falla el build si no se cumple.
- Spring Boot 4 difiere de Boot 3 (ADR 0006): antes de escribir configuración, tests o
  serialización se consulta la skill `spring-boot-4-notes`.
- Cambiar una versión mayor de esta lista requiere ADR.

## Flujo de desarrollo

- Una funcionalidad = una spec = una rama `NNN-nombre` = un PR.
- Ciclo obligatorio: `/speckit-specify` → `/speckit-clarify` → `/speckit-plan` →
  `/speckit-tasks` → `/speckit-analyze` → `/speckit-implement` → `/review-spec` → `/pr` →
  `/log-practica`. Hay aprobación humana tras clarify y tras analyze.
- Commits en inglés con Conventional Commits, sin líneas `Co-Authored-By` ni de atribución.
- Un PR solo se fusiona con CI en verde (formato → tests → arquitectura → build → imagen) **y**
  con el Quality Gate de SonarCloud aprobado; ambos son checks obligatorios del ruleset de `main`.
- Documentación y respuestas en español; código y nombres en inglés.

## Gobernanza

- Esta constitución prevalece sobre cualquier spec, plan, tarea o práctica; ante conflicto,
  manda la constitución.
- Enmiendas: MUST registrarse con una ADR en `docs/adr/` y una subida de versión semántica:
  - MAJOR: eliminar o redefinir un principio de forma incompatible.
  - MINOR: añadir un principio o sección, o ampliar materialmente una regla.
  - PATCH: aclaraciones y redacción sin cambio de significado.
- Cumplimiento: `/speckit-analyze` contrasta spec, plan y tareas con estos principios, y el
  subagente `architecture-reviewer` lo verifica en `/review-spec`. Un incumplimiento sin
  justificación en "Complexity Tracking" bloquea el PR.
- Guía operativa del día a día: `CLAUDE.md` y las skills de `.claude/skills/`.

**Version**: 1.2.0 | **Ratified**: 2026-11-12 | **Last Amended**: 2026-11-23
