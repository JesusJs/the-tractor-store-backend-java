# Requisitos del backend

Resumen de la *Guía de aprendizaje Tractor Store* (sesión 2, fases B1–B12; sesión 3, fases C1–C8), filtrado por las decisiones del proyecto. La guía completa (PDF) es la referencia; este archivo es lo que la IA debe leer.

## Funcionales
1. Servir el catálogo oficial del reto: categorías `classic` y `autonomous`, productos con variantes (SKU, color, precio, imagen) y highlights, tiendas físicas.
2. Recomendaciones por **distancia de color** a los SKUs dados.
3. Stock por SKU; el AddToCart muestra "out of stock".
4. Carrito anónimo por cookie de sesión: añadir (suma cantidad), quitar un SKU, ver carrito y mini-cart. **Desviación temporal** hasta la spec 005\* `carrito-servidor` (ver la tabla de desviaciones).
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
| Grafana, k6, caché | Fuera de alcance por plazo | — |
| SonarCloud (C2) con Quality Gate propio | SonarCloud con Sonar way (cobertura nueva ≥ 80 %; el plan gratuito no admite Quality Gate propio) | 0010 |
| Proyecto SonarCloud `tractor-store-backend` en la organización de Quind (C2) | Organización personal `jesusjs` (plan gratuito, repositorio público; acuerdo de meet4) | 0010 |
| Fly.io / Render (C5) | Azure Container Apps + Terraform + OIDC | 0005 |
| Paquetes `api`/`internal` (B9) | `api` + capas hexagonales internas | 0001 |
| Módulo Cart en el backend (sesión 2, tabla *Módulo Cart*): `GET /cart`, `GET /cart/mini`, `POST /cart/items` y `DELETE /cart/items/{sku}` por cookie; en la fase 7, `CartService` del front consumiéndolos | **Temporal**, por decisión del mentor (review del PR #5, A01): el carrito vive en `localStorage` del front y `GET /cart` solo crea `ts_session` y devuelve un carrito vacío. Efecto: en 7 de 23 productos (AU-02, AU-03, AU-04, AU-06, AU-07, CL-01, CL-08) el carrito muestra el precio "desde" y el recibo cobra el de la variante. **La cierra la spec 005\* `carrito-servidor` (2026-12-01)**: los 4 endpoints con `ts_session`, precio real de la variante y stock, y el `CartService` del front consumiéndolos ([issue del front](https://github.com/JesusJs/the-tractore-store/issues/1)). Mitigación para la demo: SKUs cuyo precio coincide con el "desde" (`specs/004-inventario-pedidos/quickstart.md` §5.1) | 0011 |
