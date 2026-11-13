# 0002 — Eventos de dominio internos con el Event Publication Registry de Spring Modulith

- Estado: aceptada
- Fecha: 2026-11-12

## Contexto
Confirmar un pedido debe vaciar el carrito, descontar el stock y notificar. Si `order` llama directamente a los otros módulos, queda acoplado a todos; si el proceso cae entre el commit y la reacción, se pierde la reacción.

## Decisión
`order` publica `OrderPlaced` (en tiempo pasado) en `order.api`. Los consumidores usan `@ApplicationModuleListener` (transaccional, después del commit y asíncrono). Spring Modulith guarda cada publicación en la tabla del Event Publication Registry dentro de la misma transacción (patrón outbox) y la reintenta si el listener falla. Sin Kafka ni otro broker.

## Alternativas consideradas
- Llamadas directas entre módulos: acoplamiento y sin reintentos.
- `@EventListener` síncrono: un fallo del listener hace rollback del pedido.
- Kafka/RabbitMQ: complejidad operativa sin necesidad (un solo proceso).

## Consecuencias
- Positivas: añadir `notifications` no requiere tocar `order`; entrega fiable.
- Negativas: consistencia eventual (el carrito se vacía milisegundos después); la tabla de eventos va en una migración Flyway propia.
- Verificación: test con `Scenario` de Modulith y test de reintento (listener que falla una vez).
