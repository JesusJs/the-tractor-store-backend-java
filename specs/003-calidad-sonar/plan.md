# Implementation Plan: Calidad con SonarCloud (análisis de PR y Quality Gate obligatorio)

**Branch**: `003-calidad-sonar` | **Date**: 2026-11-23 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/003-calidad-sonar/spec.md`

## Summary

Cada PR contra `main` y cada push a `main` se analizan en SonarCloud desde el job `build` de
GitHub Actions, con el plugin de Maven y la cobertura **agregada** de unitarios + arquitectura +
integración (JaCoCo, un único `jacoco.exec` compartido por surefire y failsafe). El Quality Gate
es **Sonar way** (cobertura de código nuevo ≥ 80 %, duplicación ≤ 3 %, ratings A, 100 % hotspots
revisados): el plan gratuito no permite uno propio, así que el 85 % de la spec original baja al
80 % y la diferencia queda en la ADR 0010. El check que publica la app de SonarCloud pasa a ser
obligatorio en el ruleset de `main` junto a `build`. Sin cambios en código de producción ni en
la API. Detalle en [research.md](research.md).

## Technical Context

**Language/Version**: Java 21 (Temurin) · YAML de GitHub Actions · JSON de ruleset

**Primary Dependencies**: plugins de build nuevos y con versión fijada: `jacoco-maven-plugin`
0.8.16 y `sonar-maven-plugin` 5.9.0.7382 (no los gestiona el parent de Spring Boot 4.1.2).
**Sin dependencias de runtime nuevas.**

**Storage**: N/A (sin tablas ni migraciones)

**Testing**: suites existentes (surefire `*Test`/`*Tests`, failsafe `*IT` con Testcontainers);
la validación de esta spec es de pipeline (ver [quickstart.md](quickstart.md))

**Target Platform**: GitHub Actions (`ubuntu-latest`) + SonarCloud (`https://sonarcloud.io`,
org `jesusjs`, proyecto `JesusJs_the-tractor-store-backend-java`)

**Project Type**: web-service (monolito modular) — esta spec toca build y CI

**Performance Goals**: job `build` de un PR ≤ +5 min respecto a hoy (SC-006)

**Constraints**: orden de pasos del CI intacto (FR-016); token solo como secreto (FR-014);
plan gratuito → Quality Gate Sonar way; demo el 2026-11-25

**Scale/Scope**: 1 `pom.xml`, 1 workflow, 1 ruleset, 1 ADR, enmienda de constitución,
`TESTING.md` nuevo, skill `testing-strategy`, `backlog.md` y `requisitos.md`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo cumple este plan | Estado |
|---|---|---|
| I. Contrato primero | No toca la API ni `docs/api-contract.md`. | ✅ (n/a) |
| II. Monolito modular + hexagonal | Sin clases nuevas; `ModularityTests` y ArchUnit siguen en `./mvnw test`. | ✅ |
| III. Pruebas primero | No hay código de producción. Las suites existentes alimentan la cobertura; la validación es el PR de prueba de SC-002 (rojo sin tests → verde con tests). Enforcer anti-H2 intacto. | ✅ |
| IV. Datos | Sin cambios; migraciones excluidas solo de cobertura/duplicación. | ✅ (n/a) |
| V. Eventos internos | No aplica. | ✅ (n/a) |
| VI. Seguridad proporcionada | `SONAR_TOKEN` solo como secreto en `env` de un paso; nunca en pom, workflow ni `-D`; gitleaks sigue primero. Check de Sonar fijado a la app (`integration_id`). | ✅ |
| VII. Simplicidad y plazo | **Viola la lista de fuera de alcance** (SonarCloud). Se resuelve con ADR 0010 + constitución v1.2.0 como primeras tareas (FR-001); ver Complexity Tracking. Resto mínimo: un job, un `.exec`, sin `qualitygate.wait`, sin umbral local extra. | ⚠️ → ✅ tras ADR 0010 |
| VIII. Entregable y operable | Imagen, health y despliegue sin cambios. | ✅ |
| Restricciones técnicas | Sin cambios de versión mayor; plugins de build nuevos con versión exacta. La v1.2.0 los añade a la lista. | ✅ |
| Flujo | Rama `003-calidad-sonar`, un PR, conventional commits; ADR antes de tocar código. La v1.2.0 añade "Quality Gate aprobado" a la condición de merge. | ✅ |

**Resultado**: pasa con una desviación justificada (principio VII), condicionada a aceptar
ADR 0010 y aplicar la enmienda v1.2.0 antes de cualquier cambio en `pom.xml` o el workflow.

**Re-check post-diseño (Fase 1)**: `data-model.md` no añade tablas; `contracts/` describe solo
checks y disparadores del CI, sin tocar la API; `quickstart.md` usa comandos existentes
(`./mvnw`, `gh`). Sigue igual: ✅ con la desviación de VII justificada.

## Project Structure

### Documentation (this feature)

```text
specs/003-calidad-sonar/
├── plan.md              # este fichero
├── research.md          # Fase 0 (R1–R10)
├── data-model.md        # Fase 1 (configuración y estado externo)
├── quickstart.md        # Fase 1 (validación)
├── contracts/
│   └── ci-quality-check.md
├── checklists/requirements.md
└── tasks.md             # Fase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
pom.xml                                   # jacoco (prepare-agent + report), sonar plugin, propiedades sonar.*
.github/workflows/ci.yml                  # paso "Quality analysis" al final del job build
docs/github/ruleset-main.json             # + check de SonarCloud obligatorio
docs/adr/0010-sonarcloud-en-alcance-sonar-way.md   # propuesta → aceptada
.specify/memory/constitution.md           # v1.1.0 → v1.2.0
docs/backlog.md, docs/requisitos.md       # SonarCloud sale de "fuera de alcance"
TESTING.md                                # nuevo (FR-011)
.claude/skills/testing-strategy/SKILL.md  # 80 % código nuevo + exclusiones (FR-012)
```

**Structure Decision**: un solo proyecto Maven; ningún fichero bajo `src/` cambia.

## Orden de implementación

1. **Gobierno** (FR-001): aceptar ADR 0010; constitución v1.2.0; `backlog.md` y `requisitos.md`.
2. **Cobertura local** (FR-008, FR-010): JaCoCo en `pom.xml` → `./mvnw clean verify` genera el
   informe con un adaptador de persistencia cubierto y las exclusiones fuera (quickstart §1).
3. **Análisis** (FR-003, FR-005, FR-014): plugin y propiedades Sonar en `pom.xml`; paso
   "Quality analysis" en `ci.yml` con la condición de R4.
4. **Documentación** (FR-011, FR-012, FR-015): `TESTING.md` (tipos de test, informe local,
   exclusiones, Quality Gate y diferencia 85→80, regla < 20 líneas, línea base, falsos
   positivos/hotspots, caída del servicio, rotación del token, `@{argLine}`) y skill.
5. **Primer análisis**: push + PR; anotar el nombre real del check (quickstart §3).
6. **Bloqueo** (FR-006): ruleset activo + `docs/github/ruleset-main.json` (quickstart §4).
7. **Validación**: PR de prueba SC-002 (quickstart §5) y análisis de `main` tras el merge (§6).

Fuera del repositorio (dueño, en SonarCloud): Sonar way asignado, New Code "Previous version",
permisos de issues/hotspots (R7, R8).

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| SonarCloud, listado fuera de alcance en el principio VII | Es el objetivo de la spec 003 (fase C2 de la guía): bloqueo automático de defectos y cobertura real en cada PR | Revisión manual sola: no mide cobertura ni bloquea; `jacoco:check` solo: no mide "código nuevo" ni bugs/vulnerabilidades. Se legitima con ADR 0010 y constitución v1.2.0 |
| Plugins de build nuevos (JaCoCo, Sonar) | Necesarios para medir y enviar la cobertura agregada | No hay forma de obtener la cobertura de integración sin agente; el análisis automático del servicio no acepta cobertura |
