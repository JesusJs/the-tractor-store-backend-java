---
name: review-spec
description: Revisa la implementación de la spec actual con tres subagentes en paralelo (spec, arquitectura, seguridad) y consolida un informe. Úsala después de /speckit-implement y antes de /pr.
disable-model-invocation: true
argument-hint: "[NNN opcional; por defecto la spec de la rama actual]"
---
# /review-spec

1. Identifica la spec: argumento `$ARGUMENTS` o el prefijo `NNN` de `git branch --show-current`. Lee `specs/NNN-*/spec.md`, `plan.md` y `tasks.md`.
2. Obtén el diff contra main: `git diff main...HEAD --stat` y `git diff main...HEAD`.
3. Lanza **en paralelo, en un solo mensaje**, tres subagentes con la herramienta Agent, pasando a cada uno la ruta de la spec y la lista de archivos cambiados:
   - `spec-reviewer`
   - `architecture-reviewer`
   - `security-reviewer`
4. Ejecuta `./mvnw -q verify` (si falla, es hallazgo BLOQUEANTE).
5. Consolida en `specs/NNN-*/review.md`:
   ```
   # Revisión NNN — fecha
   | # | Severidad (BLOQUEANTE/ALTA/MEDIA/BAJA) | Revisor | Archivo:línea | Hallazgo | Propuesta |
   ```
   Elimina duplicados y descarta hallazgos que no puedas sustentar con el código.
6. Muéstrame la tabla y pregúntame cuáles corriges. No corrijas nada sin mi confirmación.
