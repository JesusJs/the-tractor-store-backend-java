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
4. Pasa a los tres subagentes, además, estos checks (patrones encontrados en las reviews humanas de los PR #3 y #5); cada incumplimiento es un hallazgo:
   - Componente nuevo en `shared` → exige ADR y registro en el principio II de la constitución.
   - Validación coherente entre body, path y query: el mismo dato (id, SKU, texto libre) usa el mismo patrón compartido en las tres entradas.
   - Cada propiedad que cambia por perfil (`application-*.yml`) tiene un test que comprueba su valor en cada perfil.
   - Tests de concurrencia con latch de arranque y timeout en cada espera (`await(…, SECONDS)`, `get(…, SECONDS)`).
   - Datos de seed no duplicados a mano en tests o scripts: se reaplica la migración que los define.
   - Los tests afirman **exactamente** lo que pide la spec ("exactamente uno" → `== 1`, no `contains`; "misma transacción" → fallo antes y después del punto crítico).
   - Cada regla ArchUnit tiene una fixture negativa por **rama** (método/clase, Spring/Jakarta, interfaz/clase), no solo por regla.
5. Ejecuta `./mvnw -q verify` (si falla, es hallazgo BLOQUEANTE).
6. Consolida en `specs/NNN-*/review.md`:
   ```
   # Revisión NNN — fecha
   | # | Severidad (BLOQUEANTE/ALTA/MEDIA/BAJA) | Revisor | Archivo:línea | Hallazgo | Propuesta |
   ```
   Elimina duplicados y descarta hallazgos que no puedas sustentar con el código.
7. Muéstrame la tabla y pregúntame cuáles corriges. No corrijas nada sin mi confirmación.
