# 0010 — SonarCloud entra en alcance con el Quality Gate Sonar way

- Estado: aceptada (dueño del repositorio, 2026-11-23)
- Fecha: 2026-11-23

## Contexto
- La constitución v1.1.0 (principio VII), `docs/backlog.md` y `docs/requisitos.md` listan
  SonarCloud como fuera de alcance.
- La spec 003 (`specs/003-calidad-sonar/`) pide que cada PR se analice en SonarCloud, que el
  merge a `main` se bloquee si falla el Quality Gate y que la cobertura sea la agregada de
  unitarios + arquitectura + integración.
- El repositorio `JesusJs/the-tractor-store-backend-java` es público. La organización
  (`jesusjs`) y el proyecto (`JesusJs_the-tractor-store-backend-java`) ya existen en
  SonarCloud, con el "análisis automático" desactivado y el secreto `SONAR_TOKEN` guardado en
  GitHub.
- **Limitación del plan gratuito** (verificada por el dueño del repositorio, 2026-11-23): solo
  permite el Quality Gate integrado **Sonar way**; no se puede crear ni asignar uno propio.

## Decisión
Se adelanta SonarCloud a alcance, solo para este backend: el pipeline de GitHub Actions analiza
cada PR contra `main` y cada push a `main` con el escáner de Maven, enviando el informe JaCoCo
agregado, y el check de SonarCloud pasa a ser obligatorio en el ruleset de `main`. El Quality
Gate es **Sonar way**.

### Diferencia aceptada respecto a la spec original
| Condición pedida (código nuevo) | Sonar way | Efecto |
|---|---|---|
| Cobertura ≥ 85 % | Cobertura ≥ 80 % | **Umbral 5 puntos más bajo** (limitación del plan) |
| 0 bugs nuevos | Rating de fiabilidad A | Equivalente: A ⇔ 0 bugs nuevos |
| 0 vulnerabilidades nuevas | Rating de seguridad A | Equivalente: A ⇔ 0 vulnerabilidades nuevas |
| Duplicación ≤ 3 % | Duplicación ≤ 3 % | Igual |
| Ratings A | Ratings A (fiabilidad, seguridad, mantenibilidad) | Igual |
| 100 % hotspots revisados | 100 % hotspots revisados | Igual |

Además, Sonar way **no aplica** las condiciones de cobertura y duplicación cuando el código
nuevo tiene menos de 20 líneas (comportamiento del servicio, no configurable en el plan
gratuito). Se acepta: los cambios tan pequeños los revisa Oscar (code owner),
cuya aprobación exige el ruleset de `main`.

**Enmienda (review del PR #3, C03, 2026-11-25)**: se añade además un umbral **global** con
`jacoco:check` en la fase `verify` (cobertura de líneas del bundle ≥ 80 %, mismas exclusiones
que el informe). No duplica el Quality Gate: Sonar way solo mide el código nuevo y no lo
evalúa por debajo de 20 líneas, mientras que la guía C1 pide un 80 % global. No se sube a 85 %
ni se añade un umbral de ramas, para no producir bloqueos que la guía no exige.

## Alternativas consideradas
- Plan de pago de SonarCloud para un Quality Gate propio al 85 % — coste no justificado para
  una demo de un mes.
- Umbral del 85 % con `jacoco:check` en el build como sustituto del Quality Gate — mide
  cobertura global o por clase, no de "código nuevo" respecto a `main`; daría resultados
  distintos al check de Sonar. (Sí se adopta, como complemento, el 80 % global de la guía:
  ver la enmienda en *Decisión*.)
- SonarQube autoalojado (Community) — fuera de alcance por la spec y por plazo; además exige
  operar un servidor.
- Seguir sin SonarCloud — no hay cobertura medida ni bloqueo automático de defectos.

## Consecuencias
- Positivas: bloqueo automático de bugs, vulnerabilidades, duplicación y hotspots sin revisar
  en código nuevo; cobertura real visible en cada PR; resumen en el PR para revisores sin
  cuenta (proyecto público).
- Negativas / deuda aceptada: cobertura de código nuevo al 80 % y no al 85 %; cambios de menos
  de 20 líneas no se miden por cobertura/duplicación; dependencia de un servicio externo (si
  cae, el merge queda bloqueado hasta que el dueño relaje el ruleset; procedimiento en
  `TESTING.md`); PRs desde forks no pueden analizarse (sin secretos).
- Enmienda: constitución v1.1.0 → **v1.2.0** (MINOR): el principio VII saca SonarCloud de la
  lista de fuera de alcance y el flujo de desarrollo exige el Quality Gate aprobado para
  fusionar. Se actualizan las listas de fuera de alcance de `docs/backlog.md` y
  `docs/requisitos.md`.
- Cómo se verifica: check de SonarCloud obligatorio en `docs/github/ruleset-main.json` y en el
  ruleset activo; PR de prueba sin tests → check rojo y merge bloqueado (SC-002); clase cubierta
  solo por un `*IT` aparece cubierta en SonarCloud (SC-003).
