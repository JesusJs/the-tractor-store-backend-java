---
name: testing-strategy
description: Estrategia de pruebas del backend (tipos, nombres, herramientas, Testcontainers, regresiones). Cárgala al escribir o planificar tests.
---
# Estrategia de pruebas

| Tipo | Herramienta | Nombre | Corre con | Qué cubre |
|---|---|---|---|---|
| Unit | JUnit + AssertJ + Mockito | `*Test` | `./mvnw test` (surefire) | dominio y casos de uso, sin Spring |
| Arquitectura | Modulith `verify()`, ArchUnit | `*Tests` | surefire | fronteras y capas |
| Slice web | `@WebMvcTest` + MockMvc + `@MockitoBean` | `*ControllerTest` | surefire | status, JSON, validación, errores |
| Slice JPA | `@DataJpaTest` + Testcontainers | `*IT` | `./mvnw verify` (failsafe) | adaptadores y consultas |
| Módulo | `@ApplicationModuleTest` + `Scenario` | `*IT` | failsafe | eventos entre módulos |
| E2E API | `@SpringBootTest(webEnvironment=RANDOM_PORT)` + Testcontainers | `*IT` | failsafe | flujo completo con cookie |

Reglas:
- PostgreSQL real con Testcontainers + `@ServiceConnection`; **nunca H2**. Un contenedor compartido (clase base o `@TestConfiguration` con `@Bean @ServiceConnection`).
- Nombres en estilo `should_<resultado>_when_<condición>`; patrón given/when/then.
- `@ParameterizedTest` para la distancia de color y los cálculos de totales.
- Cada bug de `docs/backlog.md` → test de regresión con `@DisplayName("regresión: ...")`.
- Cobertura: dos controles. El Quality Gate **Sonar way** de SonarCloud exige 80 % sobre código nuevo y bloquea el merge; `jacoco:check` en `./mvnw verify` exige 80 % de líneas **global** (regla `BUNDLE`) y rompe el build. Exclusiones (patrones literales de Sonar y JaCoCo en la tabla de `TESTING.md`): DTOs de `infrastructure/web/dto`, `ApiError`, `TractorStoreApplication` y las migraciones `db/migration`. Los paquetes `api` e `infrastructure/config`, `CorsConfiguration`, `CorsProperties` y las entidades JPA **no** se excluyen. Detalle y justificación en [`TESTING.md`](../../../TESTING.md).
- Un test que falla primero es parte de la tarea; no se escribe código de producción sin él.
