# Bitácora de prácticas de desarrollo con IA

Evidencia de cómo se construyó el backend con desarrollo dirigido por specs. Una entrada por spec (la añade `/log-practica`).

| Fecha | Spec | Práctica | Herramienta | Qué funcionó | Qué no / ajuste | Evidencia |
|---|---|---|---|---|---|---|
| 2026-11-12 | 000 | Decisiones previas antes de generar código | Claude (chat) + guía PDF | Detectar a tiempo que Boot 3.5 ya no tiene soporte y el solapamiento entre Spec Kit y el kit propio | — | ADR 0006, `CLAUDE.md` |
| 2026-11-19 | 001 | Spec Kit completo + revisión con subagentes en paralelo y pruebas negativas de los guardarraíles | `/speckit-*`, `/review-spec` (`spec-reviewer`, `architecture-reviewer`, `security-reviewer`), skills `modular-architecture` y `spring-boot-4-notes`, `/pr` | Los revisores encontraron 20 hallazgos reales (1 bloqueante: `main` sin protección; `allowedDependencies` ausentes dejaban pasar `catalog → cart.api`); inyectar clases que violan las reglas demostró que Modulith y ArchUnit fallan de verdad; la ADR 0007 rompió a tiempo el ciclo `cart ↔ order` | Con `allowEmptyShould(true)` y capas vacías las reglas ArchUnit pasaban sin comprobar nada (fixtures diferidos a 005); en Modulith 2 `allowedDependencies = {}` es necesario para cerrar un módulo; la prueba negativa de gitleaks se hizo en local para no subir secretos falsos; la evidencia de T039 se contradecía y se corrigió | `specs/001-esqueleto-modular/review.md`, `pr-evidence.md`, [PR #1](https://github.com/JesusJs/the-tractor-store-backend-java/pull/1) |
| 2026-11-24 | 002 | Contrato derivado del código real del front + revisión con hallazgos reproducidos antes de corregir | `/speckit-*` (con `/speckit-analyze` y remediaciones), skills `api-contract`, `hexagonal-architecture`, `spring-boot-4-notes`, `/adr` (0008, 0009), `/review-spec` (`spec-reviewer`, `architecture-reviewer`, `security-reviewer`) | Leer los modelos y servicios Angular destapó que el contrato documentado difería en casi todos los endpoints de catálogo (ADR 0008) antes de escribir código; el módulo técnico `shared` quedó formalizado con ADR + enmienda de la constitución (v1.1.0); cada regla ArchUnit tiene un fixture que demuestra que falla (FR-028); la revisión dio 14 hallazgos sin bloqueantes y el #1 (4xx de Spring MVC → 500 con stacktrace) se reprodujo con `curl -H "Accept: application/xml"` antes de corregirlo | La estructura hexagonal inicial era implícita y hubo que refactorizar a paquetes explícitos (`domain/port/out`, `application/port/in`, `web/controller|dto|mapper`) y actualizar la skill `hexagonal-architecture`; la regla "web no usa persistencia" dejaba pasar puertos `domain.port.out` (se cerró con fixture `webport`); 3 hallazgos se difirieron al backlog (filas 007 y 010) y 4 se aceptaron con justificación; la documentación (backlog, status de la spec) se desfasó y hubo que corregirla en la revisión | `specs/002-catalogo/review.md`, `docs/adr/0008-contrato-catalogo-segun-front.md`, `docs/adr/0009-modulo-shared-errores-web.md`, [PR #2](https://github.com/JesusJs/the-tractor-store-backend-java/pull/2) |

## Prácticas usadas (catálogo)
- **Constitución** (Spec Kit): principios que el agente debe respetar en cada plan.
- **Spec → clarify → plan → tasks → analyze → implement**: separar el qué del cómo y validar la consistencia antes de codificar.
- **Contexto mínimo por fase**: `/clear` entre fases; el estado vive en `specs/NNN/`.
- **Skills de conocimiento**: contrato, arquitectura, testing y notas de Boot 4 cargadas solo cuando hacen falta.
- **Subagentes revisores** con contexto limpio (spec, arquitectura, seguridad), lanzados en paralelo.
- **Hooks**: formato automático después de cada edición.
- **Guardarraíles ejecutables**: Modulith `verify()` + ArchUnit + tests de regresión.
- **Prompts versionados** en `docs/prompts/`.
- **Prueba negativa de guardarraíles**: introducir a propósito una violación temporal (dependencia prohibida, repositorio en `web`, secreto falso en local) y comprobar que el build falla antes de dar la regla por buena. Desde la 002, cada regla ArchUnit tiene un fixture permanente en `archfixtures/` que lo demuestra en cada build.
- **Contrato contra el consumidor real**: antes de especificar endpoints, leer los modelos y servicios del front que no cambia y contrastarlos con `docs/api-contract.md`; las diferencias se resuelven con ADR, no en el código.
- **Reproducir el hallazgo antes de corregirlo**: un hallazgo de revisión solo se da por real tras reproducirlo (`curl`, test que falla) y se cierra con un test de regresión; cada hallazgo queda marcado como corregido, diferido (con fila de backlog) o aceptado con justificación.

## Aprendizajes

### 2026-11-19 — Spec 001
- **Herramientas de build**: Maven Wrapper (`mvnw`), `pom.xml`, JUnit 5, AssertJ, Spotless y Maven Enforcer.
- **Spring Boot**: aplicación base, perfiles `dev`/`test` y configuración por variables de entorno.
- **Persistencia parcial**: PostgreSQL en Docker, Flyway con `V1__baseline.sql`, OSIV desactivado e HikariCP.
- **Arquitectura**: monolito modular con arquitectura hexagonal en cada módulo.
- **Spec Kit**: constitución del proyecto, especificación, plan técnico, tareas, implementación y validación.

### 2026-11-24 — Spec 002
- El contrato lo manda el consumidor real, no la documentación: deduje el contrato de la spec oficial y el front esperaba la forma del backend .NET. Leer los modelos TypeScript del front antes de especificar me habría ahorrado reescribir la sección de catálogo. Para el carrito lo haré primero.
- Entidad de dominio ≠ entidad JPA: el dominio es Java puro y la entidad JPA es un detalle del adaptador de persistencia. Separarlos en paquetes explícitos (domain/model vs infrastructure/persistence/entity) me hizo entender la hexagonal mejor que la teoría.
- Una regla de arquitectura que no puede fallar no protege nada: los fixtures en archfixtures/ demuestran que cada regla de ArchUnit detecta su violación. Encontré un hueco real en "web no usa persistencia" gracias a eso.
- Revisar con subagentes especializados (spec, arquitectura, seguridad) encontró 14 hallazgos que yo no habría visto, pero decidir cada uno (corregir / diferir / aceptar con justificación) sigue siendo mi trabajo; reproducirlos con curl antes de corregir evitó arreglar cosas que no estaban rotas.
- El entorno también es parte del trabajo: /etc/hosts, Burp en 8080 y un JRE sin javac me costaron más tiempo que algunas features. Diagnosticar entorno en el chat y dejar a Claude Code solo para código ahorra tokens.
- Fusionar specs (persistencia + errores + catálogo) fue correcto por plazo, pero la documentación se quedó atrás de la implementación: debo actualizar docs en el mismo bloque que el código.
