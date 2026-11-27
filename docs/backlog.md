# Backlog en specs

> Fechas acordadas el 2026-11-26, sujetas a confirmación con el mentor por reasignación del autor al área de Innovación.
> Replanificación pedida en la review del PR #3 (C17) y del PR #5 (A02): demo y cierre el **2026-12-03**.

Cada fila es una ejecución del ciclo Spec Kit. El número final lo asigna `/speckit-specify` (`specs/NNN-nombre/`).
"Fases guía" = fases de la guía de aprendizaje que se practican en esa spec.

| # | Spec | Contenido | Fases guía | Ex-.NET | Día objetivo |
|---|---|---|---|---|---|
| 000 | Kit IA | `specify init`, constitución, skills, subagentes, hook, ADRs iniciales | — | — | 2026-11-12 (hecho) |
| 001 | `esqueleto-modular` | Proyecto Boot 4.1 + Maven, 5 módulos vacíos con capas hexagonales, `Modulith.verify()` + ArchUnit, Spotless, docker-compose con Postgres, Actuator health, CI mínima (build + test) | B2, B3, B9 | 001, 003 | Fusionada 2026-11-19 |
| 002 | `persistencia-dataset` → **implementada como `002-catalogo`** (fusiona 004 y 005) | Flyway V2 (esquema `catalog_*`) + V3 (dataset oficial con colores), JPA, OSIV off, Testcontainers base, ArchUnit sobre entidades (`@Entity` solo en `infrastructure.persistence`, tabla con prefijo del módulo, web nunca devuelve `@Entity`). Test de que `spring.jpa.hibernate.ddl-auto` es `validate` en todos los perfiles (review 001 #10) | B4 | 002 | Fusionada 2026-11-24 |
| 003 | `calidad-sonar` → **implementada** (fuera del plan original; ADR 0010) | JaCoCo agregado (unit + IT), SonarCloud desde CI con Quality Gate Sonar way como check obligatorio del ruleset de `main`, `TESTING.md` | — | — | Fusionada 2026-11-25; PR corto de cierre (review del PR #5, A10): 2026-11-27 |
| 006\* | `despliegue-azure-esqueleto` | Dockerfile multi-stage no-root, Terraform (RG, ACR, Container Apps env + app, PostgreSQL Flexible, Log Analytics), GitHub Actions con OIDC → URL pública con `/actuator/health`. IT que arranque con el perfil `prod` sin `DB_URL` y espere el fallo (FR-012, review 001 #11). Confirmar que el contenedor solo es alcanzable por el ingress de ACA, porque `forward-headers-strategy: framework` confía en `X-Forwarded-*`, y anotarlo en el ADR 0005 (review 001 #20). **Cookie y límites (review del PR #5, A04; T075 de la 004)**: servir front y API desde el **mismo origen** con el front enrutando `/api/*` (ADR 0004); si no es posible, `ts_session` con `SameSite=None; Secure` y CORS con el origen exacto del front y credenciales (si no, `POST /orders` crea otra sesión y la página de gracias da 404; research R4 de la 004). En el ingress: límite de frecuencia de `POST /orders` (un solo `POST` anónimo con `CL-04-BL ×10` vacía el stock) y límite de tamaño del cuerpo de **64 KB**; cabecera `X-Content-Type-Options: nosniff`. Con P3 en marcha y varias réplicas, revisar `republish-outstanding-events-on-restart` (research R13) | B12, C4, C5 | 012 (parte) | 2026-12-02 (plan B: demo local con `docker compose`) |
| 004 | `errores-validacion` — **fusionada en 002** | Formato de error único + traceId, `@RestControllerAdvice`, Bean Validation, jerarquía de excepciones | B5, B6 | 004 | — |
| 005 | `catalogo` — **fusionada en 002** | home, categories, products, stores, recommendations por color + Swagger. Quitar `allowEmptyShould(true)` de `HexagonalArchitectureTests` en cuanto `catalog.domain` tenga clases, para que una regla mal escrita no pase en verde sin comprobar nada. Test con *fixtures* que violen cada `ArchRule` y esperen `AssertionError`, para que la prueba negativa entre en el build (review 001 #4). Reglas nuevas: `api` no depende de `domain`/`application`/`infrastructure` (#7); `domain` no depende de `application`, `application` solo usa de Spring `@Service`/`@Transactional` y `@Transactional` solo en `application` (#15) | B1, B5 | 005 | — |
| 006 | `inventario` — **fusionada en `inventario-pedidos` (004\*)** | Ver 004\* | B4, B9 | — | — |
| 007 | `carrito-sesion` — **fusionada en `inventario-pedidos` (004\*)** (reducida por la ADR 0011) | Sin carrito en servidor: solo `GET /cart` que crea `ts_session` y devuelve un `Cart` vacío. Las reglas ArchUnit #3 y #12 de la review 002 pasan a la 004\* | B8 (adaptada), B9 | 006, 010 | — |
| 008 | `pedidos-eventos` — **fusionada en `inventario-pedidos` (004\*)** | Ver 004\* | B9, B10 | 007, 008 | — |
| 004* | `inventario-pedidos` (fusiona 006, 007 y 008; ADR 0011) | **P1** `GET /inventory/{sku}` → `{ sku, stock }` con stock sembrado por migración (al menos un SKU a 0) y `GET /cart` que crea `ts_session` (componente de sesión en `shared`) y devuelve un `Cart` vacío. **P2** `POST /orders` y `GET /orders/{id}` con la forma `OrderReceipt` del front: precio de `catalog.api`, IVA 21 % `HALF_UP`, stock validado y descontado de forma síncrona por `inventory.api` con `UPDATE` condicional en la misma transacción, ligado a sesión (otra sesión → 404). **P3** `OrderPlaced` con `spring-modulith-starter-jpa` + migración Flyway del Event Publication Registry (DDL PostgreSQL de la versión exacta de Modulith; pospuesto desde 001, ver `specs/001-esqueleto-modular/research.md` R2); solo `notifications` lo escucha. Reglas ArchUnit: review 002 #3 (`@ElementCollection`/`@ManyToMany` sin `@CollectionTable`/`@JoinTable` explícito y `@SecondaryTable` fallan la regla de prefijo, con fixture) y #12 (`domain` sin `jakarta..`/`io.swagger..`; toda interfaz-puerto de `domain` en `domain.port.out`); review 001 #15 (`domain` no depende de `application`, `application` solo usa de Spring `@Service`/`@Transactional`, `@Transactional` solo en `application`) | B4, B8 (adaptada), B9, B10 | 007, 008, 010 | PR #5, fusión 2026-11-26/27 |
| — | Continuación de 004\* (rama `004-eventos-pedidos`, sin número nuevo) | Fases 5–7 diferidas de `specs/004-inventario-pedidos/tasks.md` (review 004). **Fase 5 hecha** en la rama (guardas ArchUnit FR-028..FR-030 con fixtures negativas, T055–T061, y el comentario de `ArchitectureRules`; review del PR #5, A11); falta **P3** `OrderPlaced` con outbox (`spring-modulith-starter-jpa`, `V6` del Event Publication Registry) y listener en `notifications` (T062–T073), y pulido (T074, T076–T078 y el riesgo R13 de republicación con varias réplicas; T075/R4 pasa a la fila 006\*) | B9, B10 | 008 | 2026-11-30 |
| 005\* | `carrito-servidor` (review del PR #5, A01: decisión del mentor; cierra la desviación temporal de la ADR 0011) | Los 4 endpoints de la guía (`GET /cart`, `GET /cart/mini`, `POST /cart/items` suma cantidad, `DELETE /cart/items/{sku}`) ligados a `ts_session`, con precio real de la variante (`catalog.api`) y stock (`inventory.api`); el `POST /orders` parte del carrito del servidor y lo vacía. En el front, `CartService` los consume en lugar de `localStorage` ([issue del front](https://github.com/JesusJs/the-tractore-store/issues/1)). Regresión .NET "quitar un SKU vaciaba el carrito" con test | B8, fase 7 del front | 006, 010 | 2026-12-01 |
| 009 | `integracion-e2e` | Flujo completo con Testcontainers + integración real con el front | B7 | 009 | Por replanificar con el mentor (recortable) |
| 010 | `entrega-final` | CD a main con smoke test, logs JSON en prod, README, TESTING.md, DEPLOYMENT.md, ADRs pendientes. Fijar `actions/*` por SHA + Dependabot `github-actions` (review 001 #8; prioritario desde que el job expone `SONAR_TOKEN`, review del PR #3 C10); gitleaks descargado y ejecutado en `$RUNNER_TEMP` (PR #3 N05); test de paridad entre `sonar.coverage.exclusions` y los `<excludes>` de JaCoCo (PR #3 C02); `distributionSha256Sum` en `maven-wrapper.properties` (#9); decidir y documentar en ADR si Swagger UI/`/v3/api-docs` quedan activos en prod (#19; también review 002 #6). Migrar `SONAR_TOKEN` de token personal a un *Scoped Organization Token* (review 003 #14; PR #3 N01, FR-015 parcial). La aprobación de code owner en `main` ya se exige (PR #3 C12). Diferido de la review 004: límite de Σ cantidad por SKU en un pedido (#2; cambia la Clarification de líneas duplicadas) y prefijo `__Host-` para `ts_session` en prod (#8). El límite de frecuencia (#2) y el de tamaño del cuerpo (#9) pasan a la fila 006\* (review del PR #5, A04) | B11 (mín.), B12, C1, C7 | 011, 012 | C10 (`actions/*` por SHA + Dependabot `github-actions`): **2026-12-03** (issue #10 `mentor-condition`); resto de la fila: **2026-12-10** (sujetas a confirmación con el mentor; review del PR #9, R01) |
| — | Margen + ensayo de demo | | | 2026-12-02 – 2026-12-03 |

## Imprescindible para la demo
001, 002, 004\* (`inventario-pedidos`), 005\* (`carrito-servidor`) y 006\* (`despliegue-azure-esqueleto`) → un pedido completo desde el front desplegado.
**Plan B** si el despliegue no llega al 2026-12-02: demo local con `docker compose` (backend + Postgres) y el front en `localhost`.
Si el tiempo aprieta, recorta en este orden: 009 (dejar solo un test E2E), P3 de la 004* (eventos), 010 (CD manual; C10 no se recorta).
La P3 de la 004\* ya se recortó (review 004, 2026-11-25): pasa a la fila "Continuación de 004\*" (2026-11-30).

\* **El asterisco marca el número real de una spec (`specs/NNN-nombre/`) cuando choca con el de una fila del plan original.** Las filas sin asterisco conservan el número del plan original de 9 días (004–008 se fusionaron en otras specs; 009 y 010 aún no se han especificado). `/speckit-specify` asigna el siguiente número libre en `specs/`:
- El 004 original (`errores-validacion`) se fusionó en la 002 y el 003 lo ocupa `calidad-sonar`, así que `inventario-pedidos` es la **004\***.
- `carrito-servidor` será la **005\*** (no la 005 `catalogo` del plan, fusionada en la 002) y `despliegue-azure-esqueleto` la **006\*** (no la 006 `inventario`, fusionada en la 004\*).
- La continuación de la 004\* no consume número: sigue en la rama `004-eventos-pedidos`.
- Cuando se especifiquen 009 y 010 recibirán el siguiente número libre (previsiblemente 007\* y 008\*, que chocarían con las filas 007 y 008 del plan) y llevarán asterisco con la misma regla.

## Fuera de alcance
Keycloak/JWT, Grafana/Prometheus, OpenTelemetry completo, caché, k6, Kafka.
SonarCloud pasa a alcance con la spec 003 (ADR 0010, Quality Gate Sonar way).

## Regresiones del backend .NET que cada spec debe cubrir con test
| Bug anterior | Spec que lo cubre |
|---|---|
| Quitar un SKU vaciaba el carrito | 005\* `carrito-servidor` (hasta entonces no aplica: carrito en `localStorage`, ADR 0011) |
| Precio enviado por el cliente | 004* (`POST /orders` ignora `price`) |
| Recomendaciones sin `skus` fallaban | 002 (fusionó la 005) |
| Id de pedido devuelto no consultable | 004* |
| Líneas de pedido con nombre en vez de SKU | 004* |
| Entidades duplicadas | 002 (ArchUnit: `@Entity` solo en `<módulo>.infrastructure.persistence` + tabla con prefijo del módulo dueño) |
| Recomendaciones por categoría en vez de color | 002 (fusionó la 005) |
| Sin migraciones reales | 002 |
| Secreto en el código | 001 (gitleaks/CI) y 003-calidad-sonar (`SONAR_TOKEN` solo por secreto, SC-005) |
