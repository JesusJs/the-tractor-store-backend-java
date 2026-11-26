# Backlog en specs — plan de 9 días

Cada fila es una ejecución del ciclo Spec Kit. El número final lo asigna `/speckit-specify` (`specs/NNN-nombre/`).
"Fases guía" = fases de la guía de aprendizaje que se practican en esa spec.

| # | Spec | Contenido | Fases guía | Ex-.NET | Día objetivo |
|---|---|---|---|---|---|
| 000 | Kit IA | `specify init`, constitución, skills, subagentes, hook, ADRs iniciales | — | — | Jue 12 |
| 001 | `esqueleto-modular` | Proyecto Boot 4.1 + Maven, 5 módulos vacíos con capas hexagonales, `Modulith.verify()` + ArchUnit, Spotless, docker-compose con Postgres, Actuator health, CI mínima (build + test) | B2, B3, B9 | 001, 003 | Vie 13 |
| 002 | `persistencia-dataset` → **implementada como `002-catalogo`** (fusiona 004 y 005) | Flyway V2 (esquema `catalog_*`) + V3 (dataset oficial con colores), JPA, OSIV off, Testcontainers base, ArchUnit sobre entidades (`@Entity` solo en `infrastructure.persistence`, tabla con prefijo del módulo, web nunca devuelve `@Entity`). Test de que `spring.jpa.hibernate.ddl-auto` es `validate` en todos los perfiles (review 001 #10) | B4 | 002 | Lun 16 |
| 003 | `calidad-sonar` → **implementada** (fuera del plan original; ADR 0010) | JaCoCo agregado (unit + IT), SonarCloud desde CI con Quality Gate Sonar way como check obligatorio del ruleset de `main`, `TESTING.md` | — | — | Lun 23 |
| s/n | `despliegue-azure-esqueleto` (número al especificarla) | Dockerfile multi-stage no-root, Terraform (RG, ACR, Container Apps env + app, PostgreSQL Flexible, Log Analytics), GitHub Actions con OIDC → URL pública con `/actuator/health`. IT que arranque con el perfil `prod` sin `DB_URL` y espere el fallo (FR-012, review 001 #11). Confirmar que el contenedor solo es alcanzable por el ingress de ACA, porque `forward-headers-strategy: framework` confía en `X-Forwarded-*`, y anotarlo en el ADR 0005 (review 001 #20) | B12, C4, C5 | 012 (parte) | Lun 16 |
| 004 | `errores-validacion` — **fusionada en 002** | Formato de error único + traceId, `@RestControllerAdvice`, Bean Validation, jerarquía de excepciones | B5, B6 | 004 | Mar 17 |
| 005 | `catalogo` — **fusionada en 002** | home, categories, products, stores, recommendations por color + Swagger. Quitar `allowEmptyShould(true)` de `HexagonalArchitectureTests` en cuanto `catalog.domain` tenga clases, para que una regla mal escrita no pase en verde sin comprobar nada. Test con *fixtures* que violen cada `ArchRule` y esperen `AssertionError`, para que la prueba negativa entre en el build (review 001 #4). Reglas nuevas: `api` no depende de `domain`/`application`/`infrastructure` (#7); `domain` no depende de `application`, `application` solo usa de Spring `@Service`/`@Transactional` y `@Transactional` solo en `application` (#15) | B1, B5 | 005 | Mar 17 |
| 006 | `inventario` — **fusionada en `inventario-pedidos` (004\*)** | Ver 004\* | B4, B9 | — | Mié 18 |
| 007 | `carrito-sesion` — **fusionada en `inventario-pedidos` (004\*)** (reducida por la ADR 0011) | Sin carrito en servidor: solo `GET /cart` que crea `ts_session` y devuelve un `Cart` vacío. Las reglas ArchUnit #3 y #12 de la review 002 pasan a la 004\* | B8 (adaptada), B9 | 006, 010 | Mié 18 |
| 008 | `pedidos-eventos` — **fusionada en `inventario-pedidos` (004\*)** | Ver 004\* | B9, B10 | 007, 008 | Jue 19 |
| 004* | `inventario-pedidos` (fusiona 006, 007 y 008; ADR 0011) | **P1** `GET /inventory/{sku}` → `{ sku, stock }` con stock sembrado por migración (al menos un SKU a 0) y `GET /cart` que crea `ts_session` (componente de sesión en `shared`) y devuelve un `Cart` vacío. **P2** `POST /orders` y `GET /orders/{id}` con la forma `OrderReceipt` del front: precio de `catalog.api`, IVA 21 % `HALF_UP`, stock validado y descontado de forma síncrona por `inventory.api` con `UPDATE` condicional en la misma transacción, ligado a sesión (otra sesión → 404). **P3** `OrderPlaced` con `spring-modulith-starter-jpa` + migración Flyway del Event Publication Registry (DDL PostgreSQL de la versión exacta de Modulith; pospuesto desde 001, ver `specs/001-esqueleto-modular/research.md` R2); solo `notifications` lo escucha. Reglas ArchUnit: review 002 #3 (`@ElementCollection`/`@ManyToMany` sin `@CollectionTable`/`@JoinTable` explícito y `@SecondaryTable` fallan la regla de prefijo, con fixture) y #12 (`domain` sin `jakarta..`/`io.swagger..`; toda interfaz-puerto de `domain` en `domain.port.out`); review 001 #15 (`domain` no depende de `application`, `application` solo usa de Spring `@Service`/`@Transactional`, `@Transactional` solo en `application`) | B4, B8 (adaptada), B9, B10 | 007, 008, 010 | Mar 24 |
| s/n | Continuación de 004\* (número al especificarla) | Fases 5–7 diferidas de `specs/004-inventario-pedidos/tasks.md` (review 004): guardas ArchUnit FR-028..FR-030 con fixtures negativas (T055–T061; los 9 paths OpenAPI ya están), **P3** `OrderPlaced` con outbox (`spring-modulith-starter-jpa`, `V6` del Event Publication Registry) y listener en `notifications` (T062–T073), y pulido (T074–T078, incluido T075: riesgos R4 `SameSite=Lax` y R13 republicación con varias réplicas) | B9, B10 | 008 | Tras la demo |
| 005 | `carrito-servidor` (review del PR #5, A01: decisión del mentor; cierra la desviación temporal de la ADR 0011) | Los 4 endpoints de la guía (`GET /cart`, `GET /cart/mini`, `POST /cart/items` suma cantidad, `DELETE /cart/items/{sku}`) ligados a `ts_session`, con precio real de la variante (`catalog.api`) y stock (`inventory.api`); el `POST /orders` parte del carrito del servidor y lo vacía. En el front, `CartService` los consume en lugar de `localStorage` ([issue del front](https://github.com/JesusJs/the-tractore-store/issues/1)). Regresión .NET "quitar un SKU vaciaba el carrito" con test | B8, fase 7 del front | 006, 010 | 2026-12-01 |
| 009 | `integracion-e2e` | Flujo completo con Testcontainers + integración real con el front | B7 | 009 | Vie 20 |
| 010 | `entrega-final` | CD a main con smoke test, logs JSON en prod, README, TESTING.md, DEPLOYMENT.md, ADRs pendientes. Fijar `actions/*` por SHA + Dependabot `github-actions` (review 001 #8; prioritario desde que el job expone `SONAR_TOKEN`, review del PR #3 C10); gitleaks descargado y ejecutado en `$RUNNER_TEMP` (PR #3 N05); test de paridad entre `sonar.coverage.exclusions` y los `<excludes>` de JaCoCo (PR #3 C02); `distributionSha256Sum` en `maven-wrapper.properties` (#9); decidir y documentar en ADR si Swagger UI/`/v3/api-docs` quedan activos en prod (#19; también review 002 #6). Migrar `SONAR_TOKEN` de token personal a un *Scoped Organization Token* (review 003 #14; PR #3 N01, FR-015 parcial). La aprobación de code owner en `main` ya se exige (PR #3 C12). Test de que el perfil `prod` activa `Secure` en la cookie `ts_session` (analyze 004 G4). Diferido de la review 004: límite de frecuencia de `POST /orders` en el ingress y límite de Σ cantidad por SKU en un pedido (#2; el segundo cambia la Clarification de líneas duplicadas), prefijo `__Host-` para `ts_session` en prod (#8) y límite de tamaño del cuerpo en el ingress o Tomcat, p. ej. 64 KB (#9) | B11 (mín.), B12, C1, C7 | 011, 012 | Lun 23 – Mar 24 |
| — | Margen + ensayo de demo | | | | Mar 24 – Mié 25 |

## Imprescindible para la demo
001, 002, `despliegue-azure-esqueleto`, 004* (`inventario-pedidos`) → un pedido completo desde el front desplegado.
Si el tiempo aprieta, recorta en este orden: 009 (dejar solo un test E2E), P3 de la 004* (eventos), 010 (CD manual).
La P3 de la 004\* ya se recortó (review 004, 2026-11-25): pasa a la fila "Continuación de 004\*".

\* El número 004 original (`errores-validacion`) se fusionó en la 002 y el 003 lo ocupa `calidad-sonar`. `/speckit-specify` asigna el siguiente número libre en `specs/`: `inventario-pedidos` será la 004 si se especifica antes que `despliegue-azure-esqueleto` (que entonces será la 005).

## Fuera de alcance
Keycloak/JWT, Grafana/Prometheus, OpenTelemetry completo, caché, k6, Kafka.
SonarCloud pasa a alcance con la spec 003 (ADR 0010, Quality Gate Sonar way).

## Regresiones del backend .NET que cada spec debe cubrir con test
| Bug anterior | Spec que lo cubre |
|---|---|
| Quitar un SKU vaciaba el carrito | 005 `carrito-servidor` (hasta entonces no aplica: carrito en `localStorage`, ADR 0011) |
| Precio enviado por el cliente | 004* (`POST /orders` ignora `price`) |
| Recomendaciones sin `skus` fallaban | 002 (fusionó la 005) |
| Id de pedido devuelto no consultable | 004* |
| Líneas de pedido con nombre en vez de SKU | 004* |
| Entidades duplicadas | 002 (ArchUnit: `@Entity` solo en `<módulo>.infrastructure.persistence` + tabla con prefijo del módulo dueño) |
| Recomendaciones por categoría en vez de color | 002 (fusionó la 005) |
| Sin migraciones reales | 002 |
| Secreto en el código | 001 (gitleaks/CI) y 003-calidad-sonar (`SONAR_TOKEN` solo por secreto, SC-005) |
