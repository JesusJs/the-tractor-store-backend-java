# Contrato — endpoints operativos (001)

En 001 **no hay endpoints de negocio** (`/api/v1/...` está vacío; contrato de negocio en `docs/api-contract.md`, a partir de la 005). Este documento fija la interfaz HTTP que sí expone el esqueleto y la que expresamente **no** expone. Lo verifican `ApplicationIT`, `ReadinessWhenDatabaseDownIT` y `StartupFailFastIT`.

## Salud (Actuator) — FR-008, FR-009

Peticiones anónimas. El cuerpo es **exactamente** `{"status":"<ESTADO>"}`, sin `components` ni `details`. `Content-Type: application/vnd.spring-boot.actuator.v3+json` (o `application/json` si el cliente lo pide).

| Método y ruta | Incluye | BD arriba | BD caída tras arrancar |
|---|---|---|---|
| `GET /actuator/health` | todos los indicadores | 200 `{"status":"UP"}` | 503 `{"status":"DOWN"}` |
| `GET /actuator/health/liveness` | `livenessState` (no la BD) | 200 `{"status":"UP"}` | **200 `{"status":"UP"}`** |
| `GET /actuator/health/readiness` | `readinessState`, `db` | 200 `{"status":"UP"}` | 503 `{"status":"DOWN"}` (< 30 s) |

Sin BD al arrancar: el proceso no llega a escuchar y termina con error de conexión (FR-013, fail-fast).

## OpenAPI — FR-019

| Método y ruta | Respuesta |
|---|---|
| `GET /v3/api-docs` | 200, JSON OpenAPI 3.x con `paths` vacío (`{}`): sin rutas de negocio ni de `/actuator` |
| `GET /swagger-ui.html` | 302 → `/swagger-ui/index.html` |
| `GET /swagger-ui/index.html` | 200 HTML |

## Lo que NO existe (debe responder 404) — FR-009, FR-010

| Ruta | Motivo |
|---|---|
| `GET /api/v1/catalog/products` (y cualquier `/api/v1/**`) | sin endpoints de negocio |
| `GET /actuator/env`, `/actuator/configprops`, `/actuator/beans`, `/actuator/mappings` | solo `health` expuesto |
| `GET /actuator/info`, `/actuator/metrics`, `/actuator/modulith` | solo `health` expuesto (`info` llega en la 010) |

El cuerpo de estos 404 es el error por defecto de Spring Boot. El formato único `{ code, message, details?, traceId }` llega en la spec 004 y no se exige aquí.
