Plan técnico para la spec actual. Respeta @.specify/memory/constitution.md y carga la skill `spring-boot-4-notes`.

- Proyecto Maven generado como lo haría start.spring.io: groupId `com.tractorstore`, artifactId `tractor-store-backend`, Java 21, Spring Boot 4.1.x (último parche), Maven wrapper. Verifica las coordenadas de los starters en la documentación oficial de Boot 4 antes de escribir el `pom.xml`.
- Dependencias: webmvc, validation, data-jpa, flyway (+ flyway-database-postgresql), postgresql, actuator, Spring Modulith 2.x (BOM + starter-core + starter-jpa; test: starter-test), springdoc-openapi compatible con Boot 4, testcontainers (postgresql), ArchUnit (junit5).
- Plugins: Spotless (google-java-format) con `check` ligado a `verify`; surefire para unitarios y failsafe para `*IT`.
- Estructura `com.tractorstore.<módulo>.{api,domain,application,infrastructure}` con `package-info.java`: `@ApplicationModule` en el raíz del módulo y `@NamedInterface("api")` en `api`.
- Tests de arquitectura: `ModularityTests` (`ApplicationModules.of(TractorStoreApplication.class).verify()` + generación de documentación en `target/spring-modulith-docs`) y `HexagonalArchitectureTests` (ArchUnit: domain sin Spring/JPA/Jackson; controllers no dependen de repositorios ni de `persistence`; `infrastructure` no es accedida desde `domain`/`application`).
- `compose.yaml` con postgres:17 (puerto 5432, credenciales solo por `.env`, con `.env.example` versionado).
- Perfiles `application.yml`, `application-dev.yml`, `application-test.yml`; `spring.jpa.open-in-view=false`, `ddl-auto=validate`; Flyway con una `V1__baseline.sql` vacía (comentario) para validar el arranque.
- Un test de integración `ApplicationIT` con Testcontainers (`@ServiceConnection`) que arranca el contexto y llama a `/actuator/health`.
- `.github/workflows/ci.yml`: checkout, setup-java 21 Temurin con cache maven, `./mvnw -B verify`.
