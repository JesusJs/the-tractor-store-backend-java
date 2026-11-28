---
name: pr
description: Crea el pull request de la spec actual con la plantilla del repo. Úsala cuando la revisión está resuelta.
disable-model-invocation: true
---
# /pr

1. Verifica: rama `NNN-nombre` (no main), árbol limpio, `./mvnw -q verify` en verde, `specs/NNN-*/review.md` sin BLOQUEANTES abiertos. Si algo falla, detente y dímelo.
2. **Pasada de coherencia** (reviews de los PR #3 y #5: docs que contradecían el código o GitHub). Haz grep en `specs/NNN-*/`, `docs/`, `TESTING.md`, `README.md` y `CLAUDE.md` de las afirmaciones de estado (`pendiente`, `Draft`, `diferido`, `abierto`, `fusionado`, `en curso`, `Status`) y de los nombres de clases, métodos y hashes de commit citados. Contrasta cada una con el código (`grep`/`git log`) y con GitHub (`gh pr list --state all`, `gh api …/rulesets/<id>`). Muéstrame la lista de las que no coinciden y corrígelas en un commit `docs` antes de seguir.
3. **Condiciones del mentor abiertas**: `gh issue list --label mentor-condition --state open --json number,title,body`. Si alguna tiene la fecha límite vencida (hoy o antes), avísame con número, título y fecha y **no abras el PR** hasta que yo decida (cerrarla, replanificarla o abrir igual).
4. Commits en conventional commits (`feat(cart): ...`), en inglés, **sin** líneas `Co-Authored-By` ni firmas de herramientas.
5. `git push -u origin HEAD`.
6. Rellena `.github/pull_request_template.md` con datos reales: resumen de la spec, cambios, comandos para verlo, checklist marcado honestamente, hallazgos no aplicados de `review.md`.
7. Si la rama ya tiene un PR *draft* (regla de `TESTING.md`), actualiza su cuerpo con `gh pr edit --body-file <archivo temporal>`; si no, créalo como draft: `gh pr create --draft --base main --title "<tipo>(<módulo>): <spec>" --body-file <archivo temporal> --reviewer oscarmoraquind`. Pásalo a *ready* (`gh pr ready`) solo cuando los checks estén en verde.
8. Devuélveme la URL del PR y el estado de los checks (`gh pr checks`).
