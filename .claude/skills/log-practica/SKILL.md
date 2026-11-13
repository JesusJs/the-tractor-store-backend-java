---
name: log-practica
description: Añade a docs/practicas-ia.md la evidencia de las prácticas de IA usadas en la spec actual. Úsala al cerrar cada spec.
disable-model-invocation: true
---
# /log-practica

1. Revisa la conversación, `specs/NNN-*/` y `git log main..HEAD --oneline`.
2. Añade **una fila** a la tabla de `docs/practicas-ia.md`: fecha, spec, práctica principal, herramienta (skill, subagente, hook, comando Spec Kit), qué funcionó, qué no y qué ajustaste, evidencia (ruta o URL del PR).
3. Si apareció una práctica nueva, añádela al catálogo.
4. Pregúntame: "¿Qué aprendiste del stack en esta spec?" y añade mi respuesta en una sección `## Aprendizajes` con la fecha.
5. Haz commit `docs: log IA practices for NNN`.
