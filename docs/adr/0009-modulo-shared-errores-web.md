# 0009 — Módulo técnico `shared` para errores, `traceId` y CORS

- Estado: aceptada
- Fecha: 2026-11-20

## Contexto
La spec 002 (FR-014 – FR-019, FR-027) pide una jerarquía de errores de negocio reutilizable por `catalog`, `inventory`, `cart` y `order`, traducida a HTTP en **un único punto**, un `traceId` por petición (cabecera `X-Trace-Id` y MDC) y CORS configurado por entorno. Todo esto es transversal y no pertenece a ningún dominio.

La constitución v1.0.0 (principio II) y el ADR 0001 fijan exactamente cinco módulos. Spring Modulith trata cada paquete directo bajo `com.tractorstore` como un módulo, así que cualquier paquete compartido es, de hecho, un módulo más.

## Decisión
Se crea el módulo técnico `com.tractorstore.shared`, declarado `@ApplicationModule(displayName = "Shared", type = ApplicationModule.Type.OPEN, allowedDependencies = {})`:
- `shared.error` (Java puro, usable desde `domain`): `BusinessException`, `ErrorKind` (`NOT_FOUND`, `CONFLICT`), `NotFoundException`.
- `shared.infrastructure.web`: `ApiError`, `GlobalExceptionHandler` (`@RestControllerAdvice`), `TraceIdFilter`, `CorsProperties` y `CorsConfiguration`.

Estructura: `shared` **no** sigue la plantilla `api`/`domain`/`application`/`infrastructure` que el principio II exige a cada módulo de dominio. No tiene casos de uso ni modelo de negocio que separar en capas, y al ser `Type.OPEN` todos sus tipos son públicos, así que un paquete `api` no aportaría nada. Solo admite dos paquetes: `shared.error` (Java puro, el equivalente a un `domain` técnico) y `shared.infrastructure.web` (adaptadores web transversales). Aclaración aprobada el 2026-11-20 (hallazgo M5 de `/speckit-analyze` en la spec 002).

Reglas: `shared` no depende de ningún módulo de dominio y no contiene lógica de negocio, entidades ni tablas. Cada módulo de dominio lo declara en sus `allowedDependencies` cuando lo usa (`catalog` en la 002). La constitución pasa a v1.1.0: cinco módulos de dominio + `shared` técnico.

## Alternativas consideradas
- **Ponerlo en `catalog`**: `inventory`, `cart` y `order` dependerían de `catalog` solo por los errores, y el handler global viviría en un módulo de negocio.
- **Duplicar errores y handler en cada módulo**: viola FR-019 (un único punto de traducción) y multiplica el mantenimiento.
- **Paquete raíz sin declarar** (`com.tractorstore.error`): Modulith lo trataría igualmente como módulo, pero cerrado y sin nombre explícito; es la misma decisión, peor documentada.

## Consecuencias
- **Positivas:**
  - Un solo formato de error y un solo `@RestControllerAdvice` para toda la API.
  - Los dominios lanzan excepciones sin conocer HTTP (`ErrorKind` en vez de códigos de estado).
- **Negativas / deuda aceptada:**
  - `Type.OPEN` expone todos los tipos de `shared`; el riesgo es que crezca como cajón de sastre. Se limita a lo técnico y transversal; cualquier otra cosa necesita ADR.
- **Cómo se verifica:**
  - `ModularityTests`: `shared` sin dependencias; `catalog` solo con `shared` en `allowedDependencies`.
  - `HexagonalArchitectureTests`: `shared.error` no depende de Spring, JPA ni Jackson (lo usan los `domain`), y ninguna clase de `shared` lleva `@Entity`.
  - `GlobalExceptionHandlerTest`, `TraceIdFilterTest` y `CorsIT` (spec 002).
