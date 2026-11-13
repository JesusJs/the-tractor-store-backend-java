# Bitácora de prácticas de desarrollo con IA

Evidencia de cómo se construyó el backend con desarrollo dirigido por specs. Una entrada por spec (la añade `/log-practica`).

| Fecha | Spec | Práctica | Herramienta | Qué funcionó | Qué no / ajuste | Evidencia |
|---|---|---|---|---|---|---|
| 2026-11-12 | 000 | Decisiones previas antes de generar código | Claude (chat) + guía PDF | Detectar a tiempo que Boot 3.5 ya no tiene soporte y el solapamiento entre Spec Kit y el kit propio | — | ADR 0006, `CLAUDE.md` |

## Prácticas usadas (catálogo)
- **Constitución** (Spec Kit): principios que el agente debe respetar en cada plan.
- **Spec → clarify → plan → tasks → analyze → implement**: separar el qué del cómo y validar la consistencia antes de codificar.
- **Contexto mínimo por fase**: `/clear` entre fases; el estado vive en `specs/NNN/`.
- **Skills de conocimiento**: contrato, arquitectura, testing y notas de Boot 4 cargadas solo cuando hacen falta.
- **Subagentes revisores** con contexto limpio (spec, arquitectura, seguridad), lanzados en paralelo.
- **Hooks**: formato automático después de cada edición.
- **Guardarraíles ejecutables**: Modulith `verify()` + ArchUnit + tests de regresión.
- **Prompts versionados** en `docs/prompts/`.
