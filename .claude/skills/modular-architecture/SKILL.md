---
name: modular-architecture
description: Reglas del monolito modular con Spring Modulith 2 (módulos, API pública, eventos, verify). Cárgala al crear clases que cruzan módulos, publicar o escuchar eventos, o tocar package-info.
---
# Monolito modular con Spring Modulith

## Módulos y dependencias permitidas
| Módulo | Puede usar | Publica |
|---|---|---|
| catalog | — | — |
| inventory | eventos de order (`OrderPlaced`) | — |
| cart | catalog.api (precio, nombre, imagen), inventory.api (stock) | — |
| order | cart.api (leer y vaciar el carrito, síncrono y en la misma transacción; ADR 0007), catalog.api (tiendas) | `OrderPlaced` |
| notifications | eventos de order | — |

Escuchan `OrderPlaced`: inventory (descontar stock) y notifications (log "email enviado"). `cart` **no** lo escucha: lo vacía `order` vía `cart.api` (ADR 0007, evita el ciclo cart ↔ order).
No hay ciclos. Si necesitas una flecha nueva, crea una ADR antes.

## Mecánica
- Raíz del módulo `com.tractorstore.<m>` con `package-info.java` anotado `@org.springframework.modulith.ApplicationModule(displayName = "...")`.
- Todo subpaquete es interno por defecto. Lo público va en `<m>.api` con `@org.springframework.modulith.NamedInterface("api")` en su `package-info.java`.
- `api` contiene: interfaz `XxxApi` (implementada en `application`), records de intercambio (`VariantInfo`, `CartSnapshot`...) y eventos (`OrderPlaced`). Nunca entidades ni tipos de dominio internos.
- Referencias entre módulos **por id** (sku, storeId), nunca por objeto de otro módulo.
- Eventos: records inmutables en pasado con lo necesario para el consumidor (orderId, sessionId, líneas sku+qty). Publica con `ApplicationEventPublisher` desde el caso de uso transaccional. Consume con `@ApplicationModuleListener`.
- Event Publication Registry: `spring-modulith-starter-jpa`; crea la tabla con una migración Flyway copiando el DDL de PostgreSQL de la versión exacta de Modulith que uses (está en el jar `spring-modulith-events-jdbc`/docs; el esquema cambió en la 2.0). No actives la inicialización automática de esquema.
- Tests: `ApplicationModules.of(TractorStoreApplication.class).verify()`; `@ApplicationModuleTest` + `Scenario` para probar publicación/consumo de eventos.
