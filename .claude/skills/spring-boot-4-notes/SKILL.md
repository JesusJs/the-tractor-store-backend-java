---
name: spring-boot-4-notes
description: Diferencias de Spring Boot 4 / Spring Framework 7 / Modulith 2 frente a Boot 3 que la IA suele confundir. Cárgala antes de escribir pom.xml, configuración, tests, serialización JSON o código de Testcontainers.
---
# Notas Spring Boot 4.x (el proyecto usa 4.1.x)

Tu conocimiento de Spring es mayoritariamente de Boot 3. **Ante la duda, consulta la documentación oficial (docs.spring.io/spring-boot, la wiki "Spring Boot 4.0 Migration Guide" y las release notes de 4.1) con WebFetch antes de escribir.** Si algo no compila, no adivines: verifica.

Puntos a verificar siempre:
1. **Starters modulares.** Boot 4 separó la autoconfiguración en módulos. Hay starters específicos (p. ej. `spring-boot-starter-webmvc`, `spring-boot-starter-flyway`) y starters de test por tecnología (p. ej. para los slices de webmvc y data-jpa). Comprueba los nombres exactos en start.spring.io / la documentación de 4.1.
2. **Paquetes de los slices de test** (`@WebMvcTest`, `@DataJpaTest`, `@AutoConfigureMockMvc`) cambiaron de paquete respecto a Boot 3. Usa el import que indique la documentación de 4.1.
3. **`@MockBean` / `@SpyBean` ya no existen** → `@MockitoBean` / `@MockitoSpyBean` (`org.springframework.test.context.bean.override.mockito`).
4. **Jackson 3** es el predeterminado: paquetes `tools.jackson.*` para databind/core; las anotaciones siguen en `com.fasterxml.jackson.annotation`. Usa `JsonMapper` en vez de configurar `ObjectMapper` a mano.
5. **Testcontainers 2.x**: cambiaron los artifactIds y los paquetes de los módulos (p. ej. PostgreSQL). Comprueba las coordenadas en el BOM que gestiona Boot.
6. **Spring Modulith 2.x** para Boot 4 (1.4 era para Boot 3.5). El esquema del Event Publication Registry cambió en 2.0.
7. **springdoc-openapi**: usa la línea compatible con Boot 4 (no la 2.x de Boot 3).
8. Null-safety con anotaciones **JSpecify** (`org.jspecify.annotations.Nullable`) en el ecosistema Spring 7.
9. Framework 7 trae soporte nativo de versionado de API; **no lo usamos**: el prefijo `/api/v1` es una constante de ruta (más simple, ADR 0004).

Tras cualquier cambio en el `pom.xml`: `./mvnw -q dependency:tree | head` y `./mvnw -q verify`.
