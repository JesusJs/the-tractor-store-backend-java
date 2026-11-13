---
name: adr
description: Registra una decisión de arquitectura en docs/adr usando la plantilla. Úsala antes de cambiar un principio, el contrato o la arquitectura.
disable-model-invocation: true
argument-hint: "<título de la decisión>"
---
# /adr

1. Siguiente número = mayor `NNNN` en `docs/adr/` + 1.
2. Copia `docs/adr/0000-plantilla.md` a `docs/adr/NNNN-<slug>.md` con el título `$ARGUMENTS`.
3. Rellena contexto, decisión, alternativas (mínimo dos) y consecuencias preguntándome lo que no sepas; no inventes motivos.
4. Estado `propuesta` hasta que yo diga "aceptada".
5. Si la decisión cambia un principio, indica qué sección de `.specify/memory/constitution.md` hay que enmendar (y sugiere ejecutar `/speckit-constitution`).
