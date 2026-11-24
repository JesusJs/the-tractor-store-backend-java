Plan técnico. Respeta la constitución y las skills api-contract, hexagonal-architecture,
modular-architecture, testing-strategy y spring-boot-4-notes.

- Módulo catalog con capas hexagonales completas: dominio puro (Product, Variant, Category,
  Store, Color con distancia RGB), casos de uso, adaptadores JPA y controllers bajo /api/v1/catalog.
- Flyway: V2__catalog_schema.sql (tablas catalog_*) y V3__catalog_dataset.sql. Dinero NUMERIC(10,2).
- JPA: entidades solo en catalog.infrastructure.persistence; sin N+1 en el detalle (JOIN FETCH
  o @EntityGraph); OSIV desactivado.
- Errores: BusinessException + subclases, @RestControllerAdvice global en un paquete compartido
  permitido por Modulith, traceId en el MDC y en la cabecera X-Trace-Id,
  MethodArgumentNotValidException → 400 con detalle por campo.
- Tests: unitario parametrizado de la distancia de color; @WebMvcTest por endpoint (status +
  JSONPath); IT del adaptador JPA con Testcontainers; IT de punta a punta de los 5 endpoints.
- Deuda de la revisión 001: quitar allowEmptyShould donde ya haya clases (#4); regla ArchUnit
  para que api no exponga domain/application (#7); @Entity solo en persistence y tablas con
  prefijo del módulo; test de ddl-auto=validate (#10).
- Swagger: @Operation en los 5 endpoints.
- CORS: orígenes permitidos por variable de entorno `CORS_ALLOWED_ORIGINS` (lista separada por
  comas, con credenciales, nunca `*`); el perfil dev la rellena por defecto con
  http://localhost:4200,http://localhost:4201,http://localhost:4202,http://localhost:4203 y
  escucha en el puerto 5271 (FR-027).
- Imágenes (FR-009): antes de fijar las URLs del dataset, comprueba cómo construye hoy el front las
  URLs de imagen (usa `image` tal cual en `src`; el shell trae assets como AU-07-YE.webp) y decide
  si el backend devuelve URLs absolutas de un CDN o rutas que el front ya sirve.
