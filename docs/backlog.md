# Backlog en specs — plan de 9 días

Cada fila es una ejecución del ciclo Spec Kit. El número final lo asigna `/speckit-specify` (`specs/NNN-nombre/`).
"Fases guía" = fases de la guía de aprendizaje que se practican en esa spec.

| # | Spec | Contenido | Fases guía | Ex-.NET | Día objetivo |
|---|---|---|---|---|---|
| 000 | Kit IA | `specify init`, constitución, skills, subagentes, hook, ADRs iniciales | — | — | Jue 12 |
| 001 | `esqueleto-modular` | Proyecto Boot 4.1 + Maven, 5 módulos vacíos con capas hexagonales, `Modulith.verify()` + ArchUnit, Spotless, docker-compose con Postgres, Actuator health, CI mínima (build + test) | B2, B3, B9 | 001, 003 | Vie 13 |
| 002 | `persistencia-dataset` | Flyway V1 (esquema por módulo) + V2 (dataset oficial con colores), JPA, OSIV off, Testcontainers base, ArchUnit sobre entidades (`@Entity` solo en `infrastructure.persistence`, tabla con prefijo del módulo, web nunca devuelve `@Entity`) | B4 | 002 | Lun 16 |
| 003 | `despliegue-azure-esqueleto` | Dockerfile multi-stage no-root, Terraform (RG, ACR, Container Apps env + app, PostgreSQL Flexible, Log Analytics), GitHub Actions con OIDC → URL pública con `/actuator/health` | B12, C4, C5 | 012 (parte) | Lun 16 |
| 004 | `errores-validacion` | Formato de error único + traceId, `@RestControllerAdvice`, Bean Validation, jerarquía de excepciones | B5, B6 | 004 | Mar 17 |
| 005 | `catalogo` | home, categories, products, stores, recommendations por color + Swagger | B1, B5 | 005 | Mar 17 |
| 006 | `inventario` | `GET inventory/{sku}` + API pública de stock para cart | B4, B9 | — | Mié 18 |
| 007 | `carrito-sesion` | Cookie `ts_session`, 4 endpoints de cart, precio desde catalog, CORS | B8 (adaptada), B9 | 006, 010 | Mié 18 |
| 008 | `pedidos-eventos` | POST/GET orders ligado a sesión, `OrderPlaced` con Event Publication Registry → cart se vacía, inventory descuenta, notifications registra | B9, B10 | 007, 008 | Jue 19 |
| 009 | `integracion-e2e` | Flujo completo con Testcontainers + integración real con el front | B7 | 009 | Vie 20 |
| 010 | `entrega-final` | CD a main con smoke test, logs JSON en prod, README, TESTING.md, DEPLOYMENT.md, ADRs pendientes | B11 (mín.), B12, C1, C7 | 011, 012 | Lun 23 – Mar 24 |
| — | Margen + ensayo de demo | | | | Mar 24 – Mié 25 |

## Imprescindible para la demo
001, 002, 003, 005, 007, 008 → un pedido completo desde el front desplegado.
Si el tiempo aprieta, recorta en este orden: 009 (dejar solo un test E2E), 006 (stock fijo "en stock"), 010 (CD manual).

## Fuera de alcance
Keycloak/JWT, Grafana/Prometheus, OpenTelemetry completo, caché, k6, SonarCloud, Kafka.

## Regresiones del backend .NET que cada spec debe cubrir con test
| Bug anterior | Spec que lo cubre |
|---|---|
| Quitar un SKU vaciaba el carrito | 007 |
| Precio enviado por el cliente | 007 |
| Recomendaciones sin `skus` fallaban | 005 |
| Id de pedido devuelto no consultable | 008 |
| Líneas de pedido con nombre en vez de SKU | 008 |
| Entidades duplicadas | 002 (ArchUnit: `@Entity` solo en `<módulo>.infrastructure.persistence` + tabla con prefijo del módulo dueño) |
| Recomendaciones por categoría en vez de color | 005 |
| Sin migraciones reales | 002 |
| Secreto en el código | 001 (gitleaks/CI) y 003 |
