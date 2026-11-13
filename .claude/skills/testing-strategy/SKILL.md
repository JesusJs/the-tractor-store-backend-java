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
- Cobertura objetivo 80% (JaCoCo), excluyendo records DTO, `config` y la clase `Application`.
- Un test que falla primero es parte de la tarea; no se escribe código de producción sin él.
