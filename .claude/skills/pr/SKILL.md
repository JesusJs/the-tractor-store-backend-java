---
name: pr
description: Crea el pull request de la spec actual con la plantilla del repo. Úsala cuando la revisión está resuelta.
disable-model-invocation: true
---
# /pr

1. Verifica: rama `NNN-nombre` (no main), árbol limpio, `./mvnw -q verify` en verde, `specs/NNN-*/review.md` sin BLOQUEANTES abiertos. Si algo falla, detente y dímelo.
2. Commits en conventional commits (`feat(cart): ...`), en inglés, **sin** líneas `Co-Authored-By` ni firmas de herramientas.
3. `git push -u origin HEAD`.
4. Rellena `.github/pull_request_template.md` con datos reales: resumen de la spec, cambios, comandos para verlo, checklist marcado honestamente, hallazgos no aplicados de `review.md`.
5. `gh pr create --base main --title "<tipo>(<módulo>): <spec>" --body-file <archivo temporal> --reviewer oscarmoraquind`.
6. Devuélveme la URL del PR y el estado de los checks (`gh pr checks`).
