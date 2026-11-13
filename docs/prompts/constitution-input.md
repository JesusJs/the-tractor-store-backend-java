Crea la constitución del proyecto **Tractor Store — backend Java** en español, versión 1.0.0, ratificada el 2026-11-12.
Usa como contexto @CLAUDE.md, @docs/requisitos.md, @docs/api-contract.md y las ADRs de @docs/adr/.

Principios (cada uno con reglas verificables y "cómo se comprueba"):

I. **Contrato primero.** `docs/api-contract.md` manda. Prefijo `/api/v1/<módulo>`. Error único `{code, message, details?, traceId}`. Ningún endpoint fuera del contrato sin ADR. Nunca se exponen entidades JPA.

II. **Monolito modular + hexagonal.** Módulos catalog, inventory, cart, order, notifications. Capas `api`, `domain`, `application`, `infrastructure`. `domain` sin imports de Spring, JPA ni Jackson. Controllers solo llaman casos de uso. Un módulo solo usa el paquete `api` o los eventos de otro. `ApplicationModules.verify()` y ArchUnit en cada build.

III. **Pruebas primero, de adentro hacia afuera (NO NEGOCIABLE).** Cada tarea de implementación empieza con un test en rojo. Dominio con unit tests sin Spring; adaptadores con slices; flujos con Testcontainers + PostgreSQL real (prohibido H2). Cada bug del backend anterior (ver `docs/backlog.md`) tiene un test de regresión.

IV. **Datos.** Cada módulo es dueño de sus tablas (prefijo). Esquema y dataset solo con Flyway; `ddl-auto=validate`. Dinero en `NUMERIC(10,2)` / `BigDecimal`. El precio siempre sale del catálogo, nunca del cliente. OSIV desactivado.

V. **Eventos internos.** Eventos en pasado, como records en el paquete `api` del publicador. Consumidores con `@ApplicationModuleListener`. Event Publication Registry de Spring Modulith (outbox). Sin brokers externos.

VI. **Seguridad proporcionada.** Sin login/JWT/Keycloak. Carrito y pedidos ligados a la cookie `ts_session` (HttpOnly, SameSite, Secure en prod); pedido de otra sesión → 404. Bean Validation en toda entrada. CORS con orígenes explícitos. Cero secretos en el repo o en la imagen.

VII. **Simplicidad y plazo.** Demo el 2026-11-25. YAGNI. Fuera de alcance: Keycloak, Grafana, OpenTelemetry completo, caché, k6, SonarCloud, Kafka. Cualquier complejidad extra se justifica en la sección "Complexity Tracking" del plan.

VIII. **Entregable y operable.** Docker multi-stage con usuario no-root. Actuator health para probes. Despliegue a Azure Container Apps con Terraform y GitHub Actions con OIDC. Configuración por perfiles y variables de entorno.

Restricciones técnicas: Java 21, Spring Boot 4.1.x, Spring Modulith 2.x, Maven wrapper, PostgreSQL, Flyway, springdoc-openapi, Spotless (google-java-format).

Flujo de desarrollo: una spec = una rama `NNN-nombre` = un PR; ciclo specify → clarify → plan → tasks → analyze → implement → /review-spec → /pr → /log-practica. Conventional commits sin líneas de co-autoría.

Gobernanza: la constitución prevalece sobre cualquier plan; enmiendas mediante ADR + subida de versión semántica de la constitución; `/speckit-analyze` y el subagente architecture-reviewer verifican el cumplimiento.
