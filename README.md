# The Tractor Store — backend Java

[![CI](https://github.com/JesusJs/the-tractor-store-backend-java/actions/workflows/ci.yml/badge.svg)](https://github.com/JesusJs/the-tractor-store-backend-java/actions/workflows/ci.yml)

Backend del [Tractor Store](https://micro-frontends.org/tractor-store/) como monolito modular (Spring Boot 4.1, Java 21, PostgreSQL, Spring Modulith), construido con desarrollo dirigido por specs (GitHub Spec Kit + Claude Code).

- Contrato de API: [`docs/api-contract.md`](docs/api-contract.md)
- Backlog: [`docs/backlog.md`](docs/backlog.md) · Decisiones: [`docs/adr/`](docs/adr/)
- Prácticas de IA: [`docs/practicas-ia.md`](docs/practicas-ia.md)
- Arranque del entorno: [`BOOTSTRAP.md`](BOOTSTRAP.md)

## Prerrequisitos

- Java 21 (Maven no hace falta: se usa el wrapper `./mvnw`).
- Docker en marcha (BD local y tests de integración con Testcontainers).

## Comandos

| Qué | Comando |
|---|---|
| Build completo: formato + unitarios + arquitectura + integración | `./mvnw verify` |
| Solo unitarios y reglas de arquitectura (rápido) | `./mvnw test` |
| Formatear el código (Google Java Format) | `./mvnw spotless:apply` |
| Levantar PostgreSQL local | `docker compose up -d postgres` |
| Arrancar la app en perfil `dev` | `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` |

### Variables de entorno

El perfil `dev` y `compose.yaml` usan valores locales por defecto (`tractor`, no secretos). Para cambiarlos, copia
[`.env.example`](.env.example) a `.env` (ignorado por git) y ajusta:

| Variable | Por defecto | Uso |
|---|---|---|
| `DB_PORT` | `5432` | Puerto publicado de PostgreSQL |
| `DB_NAME` | `tractor` | Nombre de la base de datos |
| `DB_USER` | `tractor` | Usuario |
| `DB_PASSWORD` | `tractor` | Contraseña |
| `DB_URL` | derivada de las anteriores | URL JDBC completa (obligatoria en `prod`) |

En el perfil `prod` no hay valores por defecto: si falta `DB_URL`, `DB_USER` o `DB_PASSWORD`, la app no arranca.

## URLs (con la app en marcha)

| URL | Qué es |
|---|---|
| http://localhost:8080/actuator/health | Estado global (`UP` → 200, `DOWN` → 503) |
| http://localhost:8080/actuator/health/liveness | Probe de vida (no depende de la BD) |
| http://localhost:8080/actuator/health/readiness | Probe de disponibilidad (incluye la BD) |
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/v3/api-docs | Especificación OpenAPI |

## Integración continua

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) se ejecuta en cada push y PR, con un job `build` que se detiene
en la primera etapa rota: escaneo de secretos (gitleaks, todo el historial) → formato → unitarios + arquitectura →
integración → empaquetado.
