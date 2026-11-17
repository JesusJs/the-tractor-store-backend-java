# Data model — 001 esqueleto-modular

Esta funcionalidad **no introduce datos de negocio**: no hay entidades, tablas ni migraciones con contenido (spec, *Key Entities*). Este documento describe los únicos "elementos" del diseño: los módulos, sus capas y la configuración por perfiles.

## Módulos

| Módulo | Paquete raíz | `displayName` | Tablas (desde la 002) | Dependencias permitidas en 001 |
|---|---|---|---|---|
| Catálogo | `com.tractorstore.catalog` | Catalog | `catalog_*` | ninguna |
| Inventario | `com.tractorstore.inventory` | Inventory | `inventory_*` | ninguna |
| Carrito | `com.tractorstore.cart` | Cart | `cart_*` | ninguna |
| Pedidos | `com.tractorstore.order` | Order | `order_*` | ninguna |
| Notificaciones | `com.tractorstore.notifications` | Notifications | — | ninguna |

En 001 ningún módulo depende de otro (US1-2). Las flechas permitidas a futuro están en la skill `modular-architecture` y requieren clases que aún no existen.

## Capas por módulo

| Subpaquete | Visibilidad | Anotación en `package-info.java` | Regla verificada |
|---|---|---|---|
| `<m>` (raíz) | — | `@ApplicationModule(displayName = "...")` | detectado por Modulith (ModularityTests) |
| `<m>.api` | pública | `@NamedInterface("api")` | único punto de entrada desde otros módulos (FR-002, FR-003) |
| `<m>.domain` | interna | ninguna (solo Javadoc) | sin `org.springframework..`, `jakarta.persistence..`, `com.fasterxml.jackson..`, `tools.jackson..`, `org.hibernate..` (FR-004); no depende de `infrastructure` |
| `<m>.application` | interna | ninguna | no depende de `infrastructure` |
| `<m>.infrastructure.web` | interna | ninguna | no depende de `infrastructure.persistence` ni de repositorios Spring Data (FR-005) |
| `<m>.infrastructure.persistence` | interna | ninguna | (reglas de `@Entity` en la 002) |
| `<m>.infrastructure.config` | interna | ninguna | — |

Las capas vacías no hacen fallar la verificación (`allowEmptyShould(true)`), pero los cinco módulos deben detectarse.

## Configuración por perfil

| Propiedad | `application.yml` (común) | `dev` | `test` | `prod` |
|---|---|---|---|---|
| `spring.datasource.url` | `${DB_URL}` | `${DB_URL:jdbc:postgresql://localhost:${DB_PORT:5432}/${DB_NAME:tractor}}` | `@ServiceConnection` | `${DB_URL}` (obligatoria) |
| `spring.datasource.username` / `password` | `${DB_USER}` / `${DB_PASSWORD}` | `${DB_USER:tractor}` / `${DB_PASSWORD:tractor}` | `@ServiceConnection` | obligatorias por env |
| `spring.jpa.hibernate.ddl-auto` | `validate` | heredado | heredado | heredado |
| `spring.jpa.open-in-view` | `false` | heredado | heredado | heredado |
| `spring.flyway.locations` | `classpath:db/migration` | heredado | heredado | heredado |
| `spring.datasource.hikari.connection-timeout` / `validation-timeout` | `5s` / `2s` | heredado | heredado | heredado |
| `management.endpoints.web.exposure.include` | `health` | heredado | heredado | heredado |
| `management.endpoint.health.probes.enabled` | `true` | heredado | heredado | heredado |
| `management.endpoint.health.show-details` / `show-components` | `never` / `never` | heredado | heredado | heredado |
| `management.endpoint.health.group.readiness.include` | `readinessState,db` | heredado | heredado | heredado |

Variables de entorno: `DB_URL`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`. Las comparten `compose.yaml`, `.env.example` y los perfiles. Ninguna tiene un valor real en el repositorio.

## Migraciones

| Archivo | Contenido | Motivo |
|---|---|---|
| `V1__baseline.sql` | solo un comentario SQL | valida que Flyway arranca y registra `flyway_schema_history`; el esquema real llega en la 002 |
