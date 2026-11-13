# Requisitos del backend

Resumen de la *Guía de aprendizaje Tractor Store* (sesión 2, fases B1–B12; sesión 3, fases C1–C8), filtrado por las decisiones del proyecto. La guía completa (PDF) es la referencia; este archivo es lo que la IA debe leer.

## Funcionales
1. Servir el catálogo oficial del reto: categorías `classic` y `autonomous`, productos con variantes (SKU, color, precio, imagen) y highlights, tiendas físicas.
2. Recomendaciones por **distancia de color** a los SKUs dados.
3. Stock por SKU; el AddToCart muestra "out of stock".
4. Carrito anónimo por cookie de sesión: añadir (suma cantidad), quitar un SKU, ver carrito y mini-cart.
5. Checkout: nombre, apellido y tienda de recogida (email opcional) → pedido consultable desde la página de thanks.
6. Al confirmar un pedido: el carrito se vacía, el stock se descuenta y se "envía" una notificación (log).

## No funcionales (de la guía)
- Java 21 idiomático: records para DTOs/valores, sealed y pattern matching donde aporte, `Optional` solo como retorno.
- Inyección por constructor. `@Transactional` en la capa de aplicación, nunca en controllers. OSIV desactivado.
- DTOs separados de entidades JPA; mapeo explícito.
- Flyway para todo el esquema; `ddl-auto=validate`.
- Bean Validation en entradas; manejo global de errores; traceId en logs (MDC) y respuestas.
- Pirámide de tests: unitarios sin Spring, slices (`@WebMvcTest`, `@DataJpaTest`), integración con Testcontainers, tests de arquitectura (Modulith + ArchUnit).
- Swagger UI con todos los endpoints.
- Actuator: health (liveness/readiness), info con versión (git SHA).
- Perfiles `dev`, `test`, `prod`; secretos solo por variables de entorno.
- Dockerfile multi-stage, usuario no-root, imagen < 300 MB.
- CI: formato → tests → arquitectura → build → imagen. CD: push a main → deploy → smoke test.
- Documentación: README, ADRs (mín. monolito modular, eventos internos, Flyway), TESTING.md, DEPLOYMENT.md.

## Desviaciones conscientes respecto a la guía
| Guía | Proyecto | ADR |
|---|---|---|
| Spring Boot 3.x | Spring Boot 4.1.x (3.5 sin soporte OSS desde jun-2026) | 0006 |
| `/api/catalog/...` | `/api/v1/catalog/...` (lo que llama el front) | 0004 |
| Pedidos protegidos con JWT/Keycloak (B8) | Cookie de sesión, sin login | 0004 |
| Grafana, k6, caché, SonarCloud | Fuera de alcance por plazo | — |
| Fly.io / Render (C5) | Azure Container Apps + Terraform + OIDC | 0005 |
| Paquetes `api`/`internal` (B9) | `api` + capas hexagonales internas | 0001 |
