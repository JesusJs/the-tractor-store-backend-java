# Quickstart: validar 003 — Calidad con SonarCloud

## Prerrequisitos

- Docker en marcha (Testcontainers).
- En SonarCloud: organización `jesusjs`, proyecto `JesusJs_the-tractor-store-backend-java`,
  análisis automático desactivado, Quality Gate **Sonar way** asignado, *New Code* = "Previous
  version", permisos de issues/hotspots solo para el dueño (R8).
- En GitHub: secreto `SONAR_TOKEN` y app de SonarCloud instalada en el repositorio.

## 1. Cobertura agregada en local (US2-4)

```bash
./mvnw clean verify
```

Esperado: `BUILD SUCCESS` y `target/site/jacoco/index.html`. Abrirlo y comprobar:

- `catalog.infrastructure.persistence.adapter.CategoryRepositoryAdapter` (solo lo prueba
  `CatalogPersistenceIT`) tiene líneas cubiertas → la cobertura incluye integración (SC-003).
- `catalog.application.service.*` cubiertos (unitarios).
- No aparecen `TractorStoreApplication`, `infrastructure/web/dto/**`, `ApiError` ni
  `*Configuration`.

## 2. Ningún secreto en el repo (SC-005)

```bash
grep -rn "sonar.token\|sonar.login" pom.xml .github/
```

Esperado: 0 resultados. El paso "Secret scan" del CI sigue en verde.

## 3. Análisis del PR de esta spec (US1-1, US3, SC-004)

Abrir el PR de `003-calidad-sonar` contra `main`. Esperado:

- Job `build` con el paso nuevo **Quality analysis** en verde y su log sin el valor del token.
- Check de SonarCloud en verde y comentario/resumen del Quality Gate en el PR con enlace.
- Anotar el **nombre exacto** del check para el ruleset (R6).
- En SonarCloud → *Measures → Coverage*, `CategoryRepositoryAdapter` cubierto (SC-003).
- Duración del job frente a la última ejecución de 002: ≤ +5 min (SC-006).

## 4. Check obligatorio en `main` (FR-006)

Tras añadir el check al ruleset:

```bash
gh api repos/JesusJs/the-tractor-store-backend-java/rulesets --jq '.[].id'
```

```bash
gh api repos/JesusJs/the-tractor-store-backend-java/rulesets/<ID> --jq '.rules[] | select(.type=="required_status_checks") | .parameters.required_status_checks'
```

Esperado: `build` y el check de SonarCloud; mismo contenido que `docs/github/ruleset-main.json`.

## 5. PR de prueba que debe bloquearse (US1-2, SC-002)

Rama temporal con una clase de dominio con lógica (≥ 20 líneas nuevas, para que aplique la
condición de cobertura) y **sin** tests. Esperado: check de SonarCloud rojo ("Coverage on New
Code < 80 %") y botón de merge bloqueado. Añadir los tests → verde y merge habilitado. Cerrar
el PR sin fusionar y borrar la rama.

## 6. Análisis de `main` (US4)

Tras fusionar el PR de 003: en SonarCloud, rama `main` con un análisis de la fecha del merge.

## Qué no cubre este quickstart

Caída del servicio y PRs desde forks: se validan por lectura del procedimiento en
`TESTING.md`, no se provocan.
