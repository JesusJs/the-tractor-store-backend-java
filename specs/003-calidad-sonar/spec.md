# Feature Specification: Calidad con SonarCloud (análisis de PR y Quality Gate obligatorio)

**Feature Branch**: `003-calidad-sonar`

**Created**: 2026-11-23

**Status**: Draft

**Input**: User description: "Funcionalidad calidad-sonar (fase C2 de la guía; adelanta a SonarCloud desde 'fuera de alcance'). Cada pull request analizado por SonarCloud y sin merge a main si no supera el Quality Gate; cobertura real (unitarios + integración, agregada) visible en cada PR; revisores (Oscar, Steven) ven en el propio PR los issues y el resultado del Quality Gate. Quality Gate: 0 bugs nuevos, 0 vulnerabilidades nuevas, cobertura de código nuevo ≥ 85 %, duplicación de código nuevo ≤ 3 %, ratings A en código nuevo. Análisis en cada PR y en cada push a main; check de Sonar obligatorio en el ruleset de main. Exclusiones de cobertura justificadas (DTOs sin lógica, configuración, migraciones, clase Application) y documentadas en TESTING.md. El token nunca aparece en el repo ni en logs. Fuera de alcance: análisis del front, Sonar autoalojado. Requiere ADR que mueva SonarCloud de 'fuera de alcance' a alcance."

## Clarifications

### Session 2026-11-23

- Q: ¿Se incluye "100 % de security hotspots revisados" en código nuevo, como el Quality Gate
  por defecto del servicio, o solo las condiciones listadas? → A: Se incluye. Cada hotspot nuevo
  se revisa (seguro / a corregir) en el servicio antes de fusionar.
- Q: ¿Quién puede marcar en SonarCloud un hotspot como «seguro» o un issue como «falso
  positivo»? → A: Solo el dueño del repositorio, y cada marca exige un comentario en el PR con
  enlace y justificación, que Oscar/Steven revisan antes de aprobar.

## Contexto

- La constitución (v1.1.0, principio VII) y `docs/backlog.md` / `docs/requisitos.md` listan
  SonarCloud como **fuera de alcance**. Esta spec lo adelanta, así que **antes de tocar código**
  hace falta una ADR que lo meta en alcance y la enmienda correspondiente de la constitución.
- Hoy el control de merge a `main` (`docs/github/ruleset-main.json`) exige PR y un único check
  obligatorio, `build` (gitleaks → formato → unit + arquitectura → integración → empaquetado).
- Hoy no se mide cobertura. La skill `testing-strategy` fija un objetivo orientativo del 80 %
  global; esta spec introduce un umbral **sobre código nuevo** del 85 % que sí bloquea.
- El repositorio `JesusJs/the-tractor-store-backend-java` es **público**.
- Revisores habituales (CODEOWNERS): Oscar y Steven.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Un PR que no supera el Quality Gate no se puede fusionar (Priority: P1)

Como equipo, al abrir o actualizar un pull request contra `main`, el código se analiza
automáticamente y el resultado del Quality Gate aparece como un check del PR. Si el Quality Gate
falla (por ejemplo, un bug nuevo o cobertura de código nuevo por debajo del 85 %), GitHub no
permite fusionar el PR.

**Why this priority**: es el objetivo central: que los defectos se paren antes de llegar a
`main` sin depender de la revisión manual. Sin el bloqueo, el análisis es solo informativo.

**Independent Test**: abrir un PR de prueba que añade una clase con lógica sin tests y comprobar
que el check de calidad queda en rojo y el botón de merge está bloqueado; añadir los tests y
comprobar que el check pasa a verde y el merge se habilita.

**Acceptance Scenarios**:

1. **Given** un PR contra `main` cuyo código nuevo cumple todas las condiciones del Quality Gate,
   **When** termina el análisis, **Then** el check de calidad aparece en verde y, junto con
   `build`, el PR se puede fusionar.
2. **Given** un PR que introduce código nuevo con cobertura < 85 %, **When** termina el
   análisis, **Then** el check de calidad aparece en rojo y GitHub impide el merge.
3. **Given** un PR que introduce un bug o una vulnerabilidad nuevos detectados por el análisis,
   **When** termina el análisis, **Then** el Quality Gate falla y el merge queda bloqueado.
4. **Given** un PR que introduce más de un 3 % de líneas duplicadas en código nuevo o empeora un
   rating de código nuevo por debajo de A, **When** termina el análisis, **Then** el Quality Gate
   falla.
5. **Given** un PR que introduce un security hotspot nuevo sin revisar, **When** termina el
   análisis, **Then** el Quality Gate falla; **When** un miembro del equipo lo revisa en el
   servicio y se reanaliza, **Then** el Quality Gate puede pasar.
6. **Given** un PR en el que el análisis no se ha ejecutado o no ha terminado, **When** alguien
   intenta fusionarlo, **Then** GitHub lo impide porque falta el check obligatorio.

---

### User Story 2 - Cobertura real (unitarios + integración) visible en cada PR (Priority: P2)

Como equipo, queremos que la cobertura reportada sume lo que ejercitan los tests unitarios, de
arquitectura y de integración (estos últimos contra la base de datos real en contenedor), y verla
en cada PR, tanto la del código nuevo como la global.

**Why this priority**: el umbral del 85 % de la US1 solo es fiable si la cobertura es la real;
si se midiera solo con unitarios, los adaptadores probados por integración aparecerían sin
cubrir y el Quality Gate fallaría injustamente (o se tentaría a bajar el umbral).

**Independent Test**: en el PR de esta spec, comprobar que una clase cubierta **solo** por un
test de integración (p. ej. un adaptador de persistencia) aparece como cubierta en el informe
del análisis, y que las clases excluidas no cuentan en el porcentaje.

**Acceptance Scenarios**:

1. **Given** una clase ejercitada únicamente por tests de integración, **When** se analiza el
   PR, **Then** sus líneas aparecen cubiertas.
2. **Given** una clase ejercitada únicamente por tests unitarios, **When** se analiza el PR,
   **Then** sus líneas aparecen cubiertas.
3. **Given** las categorías excluidas (DTOs sin lógica, configuración, migraciones, clase de
   arranque de la aplicación), **When** se analiza, **Then** no penalizan el porcentaje de
   cobertura y cada exclusión está justificada en `TESTING.md`.
4. **Given** un desarrollador en local, **When** ejecuta la suite completa, **Then** puede
   generar el mismo informe de cobertura agregado sin necesidad del servicio externo.

---

### User Story 3 - Los revisores ven issues y Quality Gate en el propio PR (Priority: P3)

Como revisores (Oscar, Steven), queremos ver en la conversación del PR un resumen del Quality
Gate (aprobado/fallido, bugs, vulnerabilidades, code smells, cobertura y duplicación del código
nuevo) y los issues señalados, sin tener que entrar en otra herramienta ni tener cuenta en ella.

**Why this priority**: mejora la revisión, pero el bloqueo (US1) y la medición (US2) ya aportan
el valor principal aunque el resumen no se publique.

**Independent Test**: abrir un PR con un issue conocido y comprobar que en el PR aparece el
comentario/resumen del análisis con el estado del Quality Gate y un enlace al detalle.

**Acceptance Scenarios**:

1. **Given** un PR analizado, **When** un revisor abre el PR, **Then** ve un resumen del Quality
   Gate con sus métricas de código nuevo y un enlace al detalle.
2. **Given** un PR con issues nuevos, **When** un revisor abre el PR, **Then** puede ver qué
   issues son y en qué ficheros sin necesitar credenciales propias del servicio de análisis
   (el proyecto es público).

---

### User Story 4 - `main` se analiza en cada push (Priority: P3)

Como equipo, queremos que cada push a `main` (es decir, cada merge) se analice para que `main`
sea la línea base contra la que se compara el código nuevo de los PR y su estado de calidad sea
visible.

**Why this priority**: necesario para que "código nuevo" signifique "lo que cambia respecto a
`main`", pero no bloquea nada por sí solo.

**Independent Test**: tras fusionar el PR de esta spec, comprobar que existe un análisis de la
rama `main` con fecha del merge.

**Acceptance Scenarios**:

1. **Given** un merge a `main`, **When** termina el pipeline, **Then** hay un análisis nuevo de
   `main` con su estado de Quality Gate.

---

### Edge Cases

- **Primer análisis**: el código existente (001 y 002) se convierte en línea base; el umbral de
  código nuevo no se aplica retroactivamente. Si el código existente tiene bugs o
  vulnerabilidades, se registran pero no bloquean PRs que no lo tocan.
- **PR que solo toca documentación o ficheros excluidos**: el Quality Gate debe pasar (no hay
  código nuevo que medir) y no bloquear el merge.
- **Falta el token o es inválido**: el análisis falla de forma visible (check en rojo), no se
  salta en silencio, y el mensaje de error no revela el valor del token.
- **PR desde un fork** (sin acceso a secretos): el análisis no puede ejecutarse; el check
  obligatorio queda sin satisfacer y el PR no se puede fusionar. Se acepta: el repo no recibe
  contribuciones externas.
- **Servicio de análisis caído**: el check obligatorio no llega a verde y los merges quedan
  bloqueados. El dueño del repositorio puede desactivar temporalmente la regla en el ruleset; el
  procedimiento se documenta y la excepción se registra en el PR.
- **Falsos positivos y hotspots**: solo el dueño del repositorio marca en el servicio un issue
  como falso positivo o un hotspot como seguro, y por cada marca deja en el PR un comentario con
  el enlace y la justificación; los revisores lo validan antes de aprobar. No se desactivan
  reglas globalmente sin dejarlo documentado.
- **Tests de integración fallidos**: si falla la suite, no se publica un análisis con cobertura
  parcial que pueda dar un Quality Gate engañoso.

## Requirements *(mandatory)*

### Functional Requirements

**Gobierno y alcance**

- **FR-001**: Antes de cualquier cambio de código o pipeline MUST existir una ADR aceptada que
  mueva SonarCloud de "fuera de alcance" a alcance, y la constitución MUST enmendarse (principio
  VII, con subida de versión) junto con las listas de fuera de alcance de `docs/backlog.md` y
  `docs/requisitos.md`.
- **FR-002**: El análisis MUST limitarse a este repositorio de backend; el front y cualquier
  instalación autoalojada quedan fuera.

**Análisis y Quality Gate**

- **FR-003**: Cada apertura o actualización de un PR contra `main` y cada push a `main` MUST
  disparar un análisis de calidad del código del backend en SonarCloud.
- **FR-004**: El proyecto MUST tener asignado un Quality Gate con estas condiciones sobre
  código nuevo: 0 bugs nuevos, 0 vulnerabilidades nuevas, cobertura ≥ 85 %, líneas duplicadas
  ≤ 3 %, ratings de fiabilidad, seguridad y mantenibilidad iguales a A, y 100 % de security
  hotspots revisados. Un hotspot nuevo sin revisar MUST hacer fallar el Quality Gate.
- **FR-004a**: Solo el dueño del repositorio MUST poder marcar en el servicio un hotspot como
  seguro o un issue como falso positivo / no se corregirá. Cada marca MUST ir acompañada de un
  comentario en el PR con el enlace al hotspot/issue y la justificación, que los revisores
  (Oscar, Steven) validan antes de aprobar. `TESTING.md` documenta este procedimiento (FR-011).
- **FR-005**: "Código nuevo" en un PR MUST significar el código cambiado respecto a la rama
  destino (`main`); en `main`, la línea base MUST quedar definida y documentada.
- **FR-006**: El resultado del Quality Gate MUST publicarse como check de estado del PR, y ese
  check MUST añadirse como obligatorio en el ruleset de `main`, junto al `build` existente. El
  JSON versionado `docs/github/ruleset-main.json` MUST reflejar el ruleset activo.
- **FR-007**: Un fallo del Quality Gate MUST hacer que el check quede en rojo (no en
  "neutral" ni "skipped").

**Cobertura**

- **FR-008**: La cobertura enviada al análisis MUST agregar los tests unitarios, de
  arquitectura y de integración (con base de datos real en contenedor) en un único informe.
- **FR-009**: El análisis MUST ejecutarse solo después de que todas las suites hayan pasado; si
  alguna falla, no se publica un análisis con cobertura parcial.
- **FR-010**: MUST excluirse de la cobertura únicamente: DTOs/records de intercambio sin lógica,
  clases de configuración, migraciones de base de datos y la clase de arranque de la aplicación.
  Las migraciones MUST excluirse también del cálculo de duplicación. Cualquier otra exclusión
  requiere justificación escrita.
- **FR-011**: `TESTING.md` MUST documentar: qué cubre cada tipo de test, cómo generar el informe
  de cobertura agregado en local, cada patrón de exclusión con su justificación, las condiciones
  del Quality Gate y el procedimiento ante falsos positivos o caída del servicio.
- **FR-012**: La skill `testing-strategy` MUST actualizarse para que su objetivo de cobertura
  sea coherente con el Quality Gate (85 % en código nuevo) y con las exclusiones de FR-010.

**Visibilidad en el PR**

- **FR-013**: Cada PR analizado MUST mostrar en GitHub un resumen del Quality Gate (estado y
  métricas de código nuevo) con enlace al detalle, visible para cualquier revisor sin cuenta
  en el servicio.

**Secretos**

- **FR-014**: El token del servicio de análisis MUST guardarse solo como secreto cifrado del
  repositorio en GitHub; MUST NOT aparecer en ficheros del repositorio, en la configuración
  versionada ni en logs del pipeline. El escaneo de secretos existente MUST seguir pasando.
- **FR-015**: El token MUST tener el mínimo alcance posible (análisis de este proyecto) y su
  creación/rotación MUST documentarse sin incluir su valor.

**No regresión del pipeline**

- **FR-016**: El check `build` existente y su orden (secretos → formato → unit + arquitectura →
  integración → empaquetado) MUST mantenerse; el análisis de calidad se añade sin quitar
  ninguna comprobación actual.

### Key Entities

- **Quality Gate**: conjunto de condiciones sobre código nuevo que decide aprobado/fallido.
- **Análisis**: ejecución sobre un PR o sobre `main`, con métricas (bugs, vulnerabilidades, code
  smells, hotspots, cobertura, duplicación) y estado del Quality Gate.
- **Informe de cobertura agregado**: resultado de unir la cobertura de todas las suites de test.
- **Exclusión**: patrón de ficheros que no cuenta para cobertura (y/o duplicación), con su
  justificación en `TESTING.md`.
- **Ruleset de `main`**: reglas de GitHub que exigen PR y checks obligatorios (`build` + calidad).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de los PRs contra `main` abiertos tras el merge de esta spec muestran el
  resultado del Quality Gate como check del PR.
- **SC-002**: Un PR de prueba con código nuevo sin tests (cobertura de código nuevo < 85 %) no
  puede fusionarse; el mismo PR con tests suficientes sí. Verificable con dos ejecuciones.
- **SC-003**: Una clase cubierta solo por tests de integración aparece con ≥ 1 línea cubierta en
  el informe del análisis (prueba de que la cobertura es agregada).
- **SC-004**: El PR de esta spec supera su propio Quality Gate antes de fusionarse.
- **SC-005**: Una búsqueda del valor del token en el repositorio, su historial y los logs del
  pipeline da 0 resultados; el escaneo de secretos del pipeline sigue en verde.
- **SC-006**: El tiempo total del pipeline de un PR no aumenta más de 5 minutos respecto al
  actual.
- **SC-007**: Un revisor sin cuenta en el servicio puede ver, desde el PR, el estado del Quality
  Gate y la lista de issues nuevos en menos de 1 minuto.

## Assumptions

- El dueño del repositorio crea manualmente la organización y el proyecto en SonarCloud, importa
  el repositorio y guarda el token como secreto de GitHub; la spec no automatiza la creación de
  cuentas ni maneja el valor del token.
- Al ser un repositorio público, el plan gratuito de SonarCloud es suficiente y permite asignar
  un Quality Gate propio al proyecto. Si el plan no lo permitiera, se volverá a preguntar antes
  de implementar (no se baja el 85 % sin decisión explícita).
- Se desactiva el "análisis automático" del servicio en favor del análisis desde el pipeline,
  porque es el único modo de enviar la cobertura agregada.
- El primer análisis de `main` fija la línea base: el código de las specs 001 y 002 no tiene que
  alcanzar el 85 % para que los PRs posteriores se puedan fusionar.
- Esta spec toma el número 003 por orden secuencial de `specs/`; la fila 003 del backlog
  (`despliegue-azure-esqueleto`) pasa a numerarse cuando se especifique.
- La rama `003-calidad-sonar` parte de `002-catalogo`, que aún no está fusionada en `main`; el PR
  de esta spec se abrirá (o rebasará) cuando 002 esté en `main`.
- Los revisores (Oscar, Steven) no necesitan cuenta en el servicio para ver el resultado.
- El umbral orientativo del 80 % global de la skill `testing-strategy` se sustituye por el
  85 % sobre código nuevo; no hay un segundo umbral de cobertura que falle el build en local.
