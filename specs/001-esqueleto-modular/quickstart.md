# Quickstart — validar 001 esqueleto-modular

Guía para comprobar de punta a punta que la funcionalidad cumple la spec. Contrato HTTP en [contracts/operational-endpoints.md](contracts/operational-endpoints.md); módulos y perfiles en [data-model.md](data-model.md).

## 0. Prerrequisitos

```bash
java -version        # 21.x
docker version       # motor en marcha (Testcontainers y BD local)
```

## 1. Verificación completa (US1, SC-001)

```bash
./mvnw verify
```

Esperado: `BUILD SUCCESS`. Corren surefire (`ModularityTests`, `HexagonalArchitectureTests`) y failsafe (`ApplicationIT`, `ReadinessWhenDatabaseDownIT`, `StartupFailFastIT`), y `spotless:check` pasa.

Informe de módulos: `ls target/spring-modulith-docs` muestra un `.puml` o `.adoc` por módulo. Deben ser exactamente `catalog`, `inventory`, `cart`, `order` y `notifications`, sin flechas entre ellos.

Solo unitarios y arquitectura (sin Docker): `./mvnw test`.

## 2. BD local y arranque en dev (US4, US3, SC-003, SC-004)

```bash
docker compose up -d postgres
docker compose ls                      # proyecto "tractor-store-java", running(1)
docker compose up -d postgres          # segunda vez: no falla ni duplica (US4-2)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

En otra terminal:

```bash
curl -i localhost:8080/actuator/health              # 200 {"status":"UP"}
curl -i localhost:8080/actuator/health/liveness     # 200 {"status":"UP"}
curl -i localhost:8080/actuator/health/readiness    # 200 {"status":"UP"}
curl -i localhost:8080/v3/api-docs                  # 200, "paths":{}
curl -i localhost:8080/actuator/env                 # 404
curl -i localhost:8080/api/v1/catalog/products      # 404
```

El proyecto de Compose se llama siempre `tractor-store-java` (`name:` en `compose.yaml`): no depende del nombre de la carpeta ni choca con el stack .NET. Por eso el contenedor es `tractor-store-java-postgres-1` y el volumen `tractor-store-java_tractor-pgdata`.

Swagger UI: abrir `http://localhost:8080/swagger-ui.html`.

BD caída tras el arranque (US3-3):

```bash
docker compose stop postgres
curl -i localhost:8080/actuator/health/readiness    # 503 {"status":"DOWN"}
curl -i localhost:8080/actuator/health/liveness     # 200 {"status":"UP"}
docker compose start postgres                        # readiness vuelve a 200
```

Fail-fast (US3-5): con `docker compose stop postgres`, `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` termina con error de conexión a la BD y no arranca.

Puerto ocupado: `DB_PORT=55432 docker compose up -d postgres` y `DB_PORT=55432 ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`.

## 3. Formato (US2-4, FR-006)

1. Desformatear cualquier `.java` (p. ej. quitar la indentación de `TractorStoreApplication`).
2. `./mvnw spotless:check` → falla y nombra el archivo.
3. `./mvnw spotless:apply` → `./mvnw spotless:check` en verde.

## 4. Guardas de arquitectura (US2, FR-016, SC-002)

Cada violación se introduce, se ejecuta `./mvnw test` (debe fallar con la regla rota en el mensaje) y se revierte con `git checkout -- src/ && git clean -fd src/` (vuelve a verde). Evidencia (salida del fallo) en el PR.

| Caso | Cambio temporal | Falla |
|---|---|---|
| Módulo → interno de otro (FR-003, FR-016) | `catalog/domain/Probe.java` (clase pública) usada desde `cart/application/UsesProbe.java` | `ModularityTests`: dependencia de `cart` a un tipo no expuesto de `catalog` |
| Dominio → framework (FR-004) | en `catalog/domain/Bad.java`, campo `org.springframework.stereotype.Component` o `jakarta.persistence.Entity` | `HexagonalArchitectureTests`, regla de dominio |
| Controller → repositorio (FR-005) | `catalog/infrastructure/persistence/Repo.java` + `catalog/infrastructure/web/BadController.java` que lo usa | `HexagonalArchitectureTests`, regla web→persistence |
| BD en memoria (FR-007) | añadir `com.h2database:h2` (scope test) al `pom.xml` | `maven-enforcer-plugin` (`bannedDependencies`) |

## 5. CI (US5, SC-005, FR-014, FR-015, FR-018)

- Abrir el PR de `001-esqueleto-modular`: el workflow `CI`, job `build`, termina en verde en < 10 min. Pasos visibles en orden: secret scan (con `sha256sum: OK` antes de ejecutar gitleaks) → format → unit+architecture → integration → package.
- Un commit sin formato en una rama de prueba hace fallar el paso *format* y no ejecuta los siguientes.
- Secreto (US5-4): en una rama desechable, añadir un archivo con un token de forma realista (p. ej. clave AWS de ejemplo **no válida**). El paso *secret scan* falla, muestra archivo y regla, y el valor aparece como `REDACTED`. Después se borra la rama (el escaneo de todo el historial la seguiría marcando si se fusionara).
- Protección de rama (manual, dueño del repo): GitHub → Settings → Branches → regla para `main` → *Require status checks to pass* → `build`. Comprobar que un PR en rojo no deja fusionar.
