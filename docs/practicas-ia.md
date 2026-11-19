# Bitácora de prácticas de desarrollo con IA

Evidencia de cómo se construyó el backend con desarrollo dirigido por specs. Una entrada por spec (la añade `/log-practica`).

| Fecha | Spec | Práctica | Herramienta | Qué funcionó | Qué no / ajuste | Evidencia |
|---|---|---|---|---|---|---|
| 2026-11-12 | 000 | Decisiones previas antes de generar código | Claude (chat) + guía PDF | Detectar a tiempo que Boot 3.5 ya no tiene soporte y el solapamiento entre Spec Kit y el kit propio | — | ADR 0006, `CLAUDE.md` |
| 2026-11-19 | 001 | Spec Kit completo + revisión con subagentes en paralelo y pruebas negativas de los guardarraíles | `/speckit-*`, `/review-spec` (`spec-reviewer`, `architecture-reviewer`, `security-reviewer`), skills `modular-architecture` y `spring-boot-4-notes`, `/pr` | Los revisores encontraron 20 hallazgos reales (1 bloqueante: `main` sin protección; `allowedDependencies` ausentes dejaban pasar `catalog → cart.api`); inyectar clases que violan las reglas demostró que Modulith y ArchUnit fallan de verdad; la ADR 0007 rompió a tiempo el ciclo `cart ↔ order` | Con `allowEmptyShould(true)` y capas vacías las reglas ArchUnit pasaban sin comprobar nada (fixtures diferidos a 005); en Modulith 2 `allowedDependencies = {}` es necesario para cerrar un módulo; la prueba negativa de gitleaks se hizo en local para no subir secretos falsos; la evidencia de T039 se contradecía y se corrigió | `specs/001-esqueleto-modular/review.md`, `pr-evidence.md`, [PR #1](https://github.com/JesusJs/the-tractor-store-backend-java/pull/1) |

## Prácticas usadas (catálogo)
- **Constitución** (Spec Kit): principios que el agente debe respetar en cada plan.
- **Spec → clarify → plan → tasks → analyze → implement**: separar el qué del cómo y validar la consistencia antes de codificar.
- **Contexto mínimo por fase**: `/clear` entre fases; el estado vive en `specs/NNN/`.
- **Skills de conocimiento**: contrato, arquitectura, testing y notas de Boot 4 cargadas solo cuando hacen falta.
- **Subagentes revisores** con contexto limpio (spec, arquitectura, seguridad), lanzados en paralelo.
- **Hooks**: formato automático después de cada edición.
- **Guardarraíles ejecutables**: Modulith `verify()` + ArchUnit + tests de regresión.
- **Prompts versionados** en `docs/prompts/`.
- **Prueba negativa de guardarraíles**: introducir a propósito una violación temporal (dependencia prohibida, repositorio en `web`, secreto falso en local) y comprobar que el build falla antes de dar la regla por buena.

## Aprendizajes

### 2026-11-19 — Spec 001
- **Herramientas de build**: Maven Wrapper (`mvnw`), `pom.xml`, JUnit 5, AssertJ, Spotless y Maven Enforcer.
- **Spring Boot**: aplicación base, perfiles `dev`/`test` y configuración por variables de entorno.
- **Persistencia parcial**: PostgreSQL en Docker, Flyway con `V1__baseline.sql`, OSIV desactivado e HikariCP.
- **Arquitectura**: monolito modular con arquitectura hexagonal en cada módulo.
- **Spec Kit**: constitución del proyecto, especificación, plan técnico, tareas, implementación y validación.
