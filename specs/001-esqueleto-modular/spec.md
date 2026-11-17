# Feature Specification: Esqueleto modular del backend

**Feature Branch**: `001-esqueleto-modular`

**Created**: 2026-11-13

**Status**: Draft

**Input**: User description: "Spec 001 del backlog: esqueleto-modular. Proyecto que arranca, compila y pasa sus tests desde el primer día, con los cinco módulos del dominio (catalog, inventory, cart, order, notifications) separados; endpoint de salud para las probes; build que falla ante violaciones de fronteras, dependencia del dominio con el framework, controllers que usan repositorios o código sin formatear; base de datos local con un solo comando; pipeline de CI en cada push y PR. Fuera de alcance: persistencia de negocio, migraciones con datos, despliegue a Azure."

## Clarifications

### Session 2026-11-13

- Q: Una petición anónima al endpoint de salud, ¿qué debe ver: solo el estado global o también el estado de cada componente? → A: Solo el estado global (`UP`/`DOWN`) en salud general, vida y disponibilidad; sin componentes ni detalles.
- Q: Además del endpoint de salud, ¿qué endpoints de gestión/operación expone la aplicación? → A: Solo salud (con sus grupos de vida y disponibilidad); ningún otro endpoint de gestión (variables de entorno, configuración, métricas, info…) queda expuesto en esta spec.
- Q: ¿"Un PR solo puede fusionarse en verde" exige que la rama principal tenga el pipeline como comprobación obligatoria? → A: Sí; la rama principal exige el pipeline en verde para fusionar. La configura a mano el dueño del repositorio y el PR de esta spec lo documenta como paso de verificación.
- Q: ¿El escaneo de secretos revisa solo el cambio empujado o todo el historial del repositorio? → A: Todo el historial en cada ejecución (el repositorio es pequeño y así ningún secreto antiguo queda sin detectar).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Proyecto base verificable con los cinco módulos (Priority: P1)

Como desarrollador quiero clonar el repositorio y, con un único comando de verificación, compilar el proyecto y ejecutar todos sus tests en verde, encontrando ya separados los cinco módulos del dominio (catálogo, inventario, carrito, pedidos y notificaciones), cada uno con sus capas (cara pública, dominio, aplicación, infraestructura), para que todo lo que se añada después nazca dentro de las fronteras correctas.

**Why this priority**: es el cimiento de todas las specs siguientes; sin un proyecto que compile y se verifique no se puede empezar ninguna funcionalidad.

**Independent Test**: en una máquina limpia con los prerrequisitos documentados, ejecutar el comando de verificación y comprobar que termina en verde y que el informe de módulos lista exactamente los cinco esperados.

**Acceptance Scenarios**:

1. **Given** una máquina limpia con el kit de desarrollo y el motor de contenedores instalados, **When** el desarrollador ejecuta el comando de verificación completo, **Then** el proyecto compila y todos los tests (unitarios, de arquitectura e integración) pasan.
2. **Given** el proyecto compilado, **When** se inspecciona la estructura de módulos detectada, **Then** aparecen exactamente los módulos `catalog`, `inventory`, `cart`, `order` y `notifications`, sin dependencias entre ellos.
3. **Given** el proyecto en marcha, **When** se consulta cualquier ruta de negocio (`/api/v1/...`), **Then** no existe ningún endpoint de negocio todavía (respuesta de recurso no encontrado).

---

### User Story 2 - El build protege las fronteras de la arquitectura (Priority: P1)

Como equipo queremos que el build falle automáticamente si (a) un módulo usa el interior de otro en vez de su cara pública, (b) el dominio depende del framework o de la persistencia, (c) un controller usa un repositorio en lugar de un caso de uso, o (d) el código no cumple el formato acordado; así las reglas de la constitución no dependen de la memoria de nadie.

**Why this priority**: es la razón de ser del esqueleto; el backend anterior falló por no tener fronteras verificables. Comparte P1 con la US1 porque un esqueleto sin estas guardas no cumple la constitución (principio II).

**Independent Test**: introducir a propósito cada tipo de violación, ejecutar los tests y comprobar que fallan con un mensaje que identifica la regla rota; revertir y comprobar que vuelven a verde.

**Acceptance Scenarios**:

1. **Given** el esqueleto en verde, **When** una clase del módulo `cart` usa una clase interna (fuera de la cara pública) del módulo `catalog`, **Then** la verificación de arquitectura falla señalando la dependencia ilegal; al revertir el cambio vuelve a verde.
2. **Given** el esqueleto en verde, **When** una clase de una capa de dominio importa algo del framework web, de persistencia o de serialización, **Then** la verificación de arquitectura falla.
3. **Given** el esqueleto en verde, **When** un controller depende directamente de un repositorio o adaptador de persistencia, **Then** la verificación de arquitectura falla.
4. **Given** el esqueleto en verde, **When** un archivo de código queda sin el formato acordado, **Then** la verificación falla indicando el archivo; al aplicar el formateador vuelve a verde.

---

### User Story 3 - Salud de la aplicación y su base de datos (Priority: P2)

Como operador quiero un endpoint de salud que indique si la aplicación está viva y si está lista para atender (incluida la conexión con su base de datos), para usarlo en las probes de vida y disponibilidad de la plataforma de despliegue.

**Why this priority**: lo necesita la spec 003 (despliegue) y es el primer smoke test; no bloquea el desarrollo de módulos.

**Independent Test**: levantar la base de datos local y la aplicación, consultar el endpoint de salud y ver estado "UP"; con la aplicación ya arrancada, detener la base de datos y ver que el estado de disponibilidad pasa a "DOWN".

**Acceptance Scenarios**:

1. **Given** la base de datos local levantada y la aplicación arrancada, **When** se consulta el endpoint de salud, **Then** responde con estado `UP`.
2. **Given** la aplicación arrancada, **When** se consultan los grupos de vida (liveness) y disponibilidad (readiness), **Then** ambos responden por separado con su estado global.
3. **Given** la aplicación arrancada correctamente y la base de datos que cae **después** del arranque, **When** se consulta la disponibilidad, **Then** responde `DOWN`, mientras que la vida sigue `UP` (la plataforma no debe reiniciar el contenedor por una caída de la BD).
4. **Given** la aplicación arrancada, **When** se consulta el endpoint de salud sin credenciales, **Then** la respuesta contiene solo el estado global (`UP`/`DOWN`): ni la lista de componentes ni detalles internos (cadenas de conexión, usuarios, versiones).
5. **Given** la base de datos no disponible, **When** se intenta arrancar la aplicación, **Then** el arranque falla de inmediato con un mensaje claro de conexión a la base de datos (fail-fast), en lugar de arrancar y reportar `DOWN`.
6. **Given** la aplicación arrancada, **When** se consulta cualquier otro endpoint de gestión distinto de salud (variables de entorno, configuración, métricas, info), **Then** no está disponible.

---

### User Story 4 - Base de datos local con un solo comando (Priority: P2)

Como desarrollador quiero levantar la base de datos local con un único comando y que la aplicación se conecte a ella con la configuración del perfil de desarrollo, sin editar archivos ni poner secretos en el repositorio.

**Why this priority**: necesario para el desarrollo diario y para la US3, pero los tests de integración no dependen de él (usan su propia base efímera).

**Independent Test**: en una máquina limpia, ejecutar el comando documentado, arrancar la aplicación con el perfil de desarrollo y comprobar salud `UP`.

**Acceptance Scenarios**:

1. **Given** el motor de contenedores instalado, **When** el desarrollador ejecuta el comando documentado, **Then** queda disponible una base de datos local accesible por la aplicación en perfil de desarrollo.
2. **Given** la base de datos local, **When** se repite el comando, **Then** no falla ni duplica la instancia.
3. **Given** el repositorio, **When** se buscan credenciales reales, **Then** no hay ninguna: los valores locales son de desarrollo y se pueden sobrescribir por variables de entorno.

---

### User Story 5 - Integración continua en cada push y PR (Priority: P3)

Como equipo queremos un pipeline que, en cada push y en cada pull request, compruebe formato, ejecute todos los tests (incluidos los de arquitectura e integración) y construya el proyecto, de modo que un PR solo pueda fusionarse en verde.

**Why this priority**: valida automáticamente lo anterior en cada cambio; depende de que US1 y US2 existan.

**Independent Test**: abrir el PR de esta rama y comprobar que el pipeline se ejecuta y termina en verde; empujar un commit sin formato y comprobar que falla.

**Acceptance Scenarios**:

1. **Given** el PR de esta rama, **When** se abre o actualiza, **Then** el pipeline se ejecuta y termina en verde.
2. **Given** un commit que rompe formato, tests o arquitectura, **When** se empuja, **Then** el pipeline falla en la etapa correspondiente.
3. **Given** el pipeline, **When** se ejecuta, **Then** no usa secretos de larga duración ni los imprime en el log.
4. **Given** un commit que añade un secreto al código, **When** se empuja, **Then** la etapa de escaneo de secretos lo detecta y el pipeline falla, indicando el archivo afectado sin imprimir el valor del secreto (regresión "Secreto en el código" del backend anterior).
5. **Given** un PR con el pipeline en rojo o sin terminar, **When** se intenta fusionar en la rama principal, **Then** la fusión está bloqueada (comprobación obligatoria).

---

### Edge Cases

- ¿Qué pasa si la base de datos no está disponible al arrancar? Fail-fast: la aplicación termina con un mensaje claro que indica que no pudo conectar con la base de datos (la validación de esquema de FR-013 no puede ejecutarse sin ella) y la plataforma de despliegue la reinicia. No arranca en un estado degradado.
- ¿Qué pasa si se ejecuta el comando de verificación sin motor de contenedores? Los tests de integración fallan con un mensaje claro de prerrequisito; los tests unitarios y de arquitectura siguen pudiendo ejecutarse por separado.
- ¿Qué pasa si un módulo vacío no tiene clases en alguna capa? La verificación de arquitectura no debe fallar por capas vacías, pero los cinco módulos deben ser detectados.
- ¿Qué pasa si el puerto de la base de datos local ya está ocupado? El comando falla con mensaje claro y el puerto es configurable por variable de entorno.
- ¿Qué pasa si dos módulos declaran la misma entidad de persistencia? Fuera de alcance aquí: en esta spec no existen entidades; la regla que lo impide llega en la spec 002 (ver Assumptions).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST estructurarse como un único desplegable con exactamente cinco módulos: `catalog`, `inventory`, `cart`, `order` y `notifications`.
- **FR-002**: Cada módulo MUST tener una única cara pública declarada (`api`) y capas internas de dominio, aplicación e infraestructura (web, persistencia, configuración).
- **FR-003**: La verificación MUST fallar si un módulo depende de algo de otro módulo que no sea su cara pública o sus eventos.
- **FR-004**: La verificación MUST fallar si cualquier capa de dominio depende del framework de aplicación, de la persistencia o de la serialización.
- **FR-005**: La verificación MUST fallar si un controller depende de un repositorio o adaptador de persistencia en lugar de un caso de uso.
- **FR-006**: La verificación MUST fallar si el código no cumple el formato acordado, y MUST existir un comando que lo aplique automáticamente.
- **FR-007**: La verificación MUST fallar si se introduce una base de datos en memoria para tests; los tests de integración MUST usar el mismo motor de base de datos que producción.
- **FR-008**: El sistema MUST exponer un endpoint de salud con grupos separados de vida y disponibilidad; la disponibilidad MUST incluir el estado de la base de datos y la vida MUST NOT depender de ella.
- **FR-009**: Para peticiones anónimas, el endpoint de salud y sus grupos MUST devolver solo el estado global (`UP`/`DOWN`), sin lista de componentes ni detalles. Ningún otro endpoint de gestión (entorno, configuración, métricas, info…) MUST quedar expuesto.
- **FR-010**: El sistema MUST NOT exponer ningún endpoint de negocio en esta funcionalidad.
- **FR-011**: El repositorio MUST incluir un comando único para levantar la base de datos local, idempotente, con valores de desarrollo sobrescribibles por variables de entorno.
- **FR-012**: La configuración MUST separarse por perfiles (`dev`, `test`, `prod`) y MUST NOT contener secretos reales; los valores sensibles de `prod` MUST venir solo de variables de entorno.
- **FR-013**: El esquema de base de datos MUST ser validado (nunca generado) por la aplicación al arrancar; en esta spec no hay tablas de negocio. Si la base de datos no está disponible al arrancar, el arranque MUST fallar con un mensaje claro (fail-fast); una caída posterior solo afecta a la disponibilidad (FR-008).
- **FR-014**: Un pipeline de CI MUST ejecutarse en cada push y en cada pull request, con etapas en este orden: escaneo de secretos → formato → tests (unitarios + arquitectura) → integración → build, fallando en la primera etapa rota.
- **FR-015**: El escaneo de secretos MUST revisar todo el historial del repositorio en cada ejecución y MUST hacer fallar el pipeline si detecta un secreto, sin mostrar su valor en el log. Cubre la regresión "Secreto en el código" del backlog.
- **FR-016**: Un test de arquitectura MUST demostrar la regla de FR-003 con el caso concreto "`cart` usa una clase interna de `catalog`" (validado rompiéndolo a propósito y revirtiendo, documentado en el PR).
- **FR-017**: El README (o documento equivalente) MUST listar prerrequisitos y los comandos para verificar, formatear, levantar la BD y arrancar la aplicación.
- **FR-018**: La rama principal MUST exigir el pipeline de CI en verde como comprobación obligatoria antes de fusionar; la configuración la hace a mano el dueño del repositorio y se documenta en el PR.

### Key Entities

No aplica: esta funcionalidad no introduce datos de negocio. Los únicos "elementos" son los cinco módulos y sus capas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un desarrollador sin el proyecto previamente instalado pasa de clonar el repositorio a una verificación completa en verde siguiendo solo la documentación, en menos de 15 minutos (excluyendo descargas iniciales).
- **SC-002**: El 100 % de las cuatro violaciones comprobables en esta spec (módulo→interior de otro, dominio→framework, controller→repositorio, formato) son detectadas por la verificación: introducir cada una hace fallar el build y revertirla lo devuelve a verde.
- **SC-003**: Con la base de datos local levantada, el endpoint de salud responde `UP` en menos de 1 segundo; si la base cae después del arranque, la disponibilidad pasa a `DOWN` en menos de 30 segundos sin que la vida cambie; si no está disponible al arrancar, la aplicación termina con error en lugar de arrancar.
- **SC-004**: La base de datos local está lista con 1 solo comando y ninguna edición manual de archivos.
- **SC-005**: El pipeline de CI se ejecuta en el PR de esta rama, termina en verde y tarda menos de 10 minutos.
- **SC-006**: Cero endpoints de negocio expuestos y cero secretos reales en el repositorio.

## Assumptions

- Las herramientas concretas (lenguaje, framework, verificador de módulos, formateador, motor de BD, proveedor de CI) están fijadas por la constitución y `CLAUDE.md`; esta spec no las elige, el plan las aplica.
- "Máquina limpia" = kit de desarrollo de la versión exigida por la constitución + motor de contenedores en marcha; no se requiere ninguna otra instalación.
- La construcción de la imagen de contenedor y su etapa de CI quedan para la spec 003 (Dockerfile no-root); aquí la CI termina en el build.
- El endpoint `info` con versión/git SHA queda para la spec 010 (entrega final).
- Las migraciones de esquema existen como mecanismo pero sin tablas de negocio; el esquema y el dataset llegan en la spec 002.
- Los tests de integración crean su propia base de datos efímera; no dependen de la base local de US4.
- El formato de error único y el traceId llegan en la spec 004; aquí no hay endpoints de negocio que los necesiten.
- Las reglas de arquitectura sobre entidades de persistencia se cubren en la spec 002, no aquí, porque en 001 no hay entidades y esas reglas no podrían fallar:
  - Regresión "Entidades duplicadas": las entidades de persistencia (`@Entity`) viven solo en `<módulo>.infrastructure.persistence` y su nombre de tabla lleva el prefijo del módulo dueño.
  - La capa web no devuelve entidades de persistencia en ninguna respuesta HTTP (constitución, principio I).
- Fuera de alcance: persistencia de negocio, migraciones con datos, despliegue a Azure, publicación/escucha de eventos (solo se deja el sitio previsto en la cara pública de cada módulo).
