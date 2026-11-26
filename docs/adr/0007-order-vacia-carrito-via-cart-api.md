# 0007 — `order` vacía el carrito llamando a `cart.api` de forma síncrona

- Estado: reemplazada por 0011 (2026-11-24): no hay carrito en servidor; `order` ya no depende de `cart.api`
- Fecha: 2026-11-19

## Contexto
La tabla de la skill `modular-architecture` y el ADR 0002 decían que `order` lee el carrito por `cart.api` y que `cart` escuchaba `OrderPlaced`, publicado en `order.api`, para vaciarlo. Eso son dos flechas opuestas, `order → cart :: api` y `cart → order :: api`. Spring Modulith (`ApplicationModules.verify()`) rechaza los ciclos entre módulos. La revisión de la spec 001 lo detectó al declarar `allowedDependencies` (`specs/001-esqueleto-modular/review.md`, «Observación para 008»). El build habría fallado en cuanto los dos módulos tuvieran código, en la spec 008.

## Decisión
El caso de uso de confirmar pedido en `order` vacía el carrito llamando a `cart.api`, de forma síncrona y en la misma transacción que guarda el pedido. `cart` no escucha `OrderPlaced`. Los únicos consumidores del evento son `inventory` (descontar stock) y `notifications` (log de «email enviado»).

## Alternativas consideradas
- **Evento `CheckoutRequested`.** El vaciado seguiría llegando por un evento, así que sería asíncrono y el checkout dejaría de ser atómico, que es lo que se quiere conseguir.
- **Módulo compartido** del que dependan `cart` y `order`. Añade un sexto módulo a los cinco que fijan el ADR 0001 y `CLAUDE.md`.

## Consecuencias
- **Positivas:**
  - El checkout es atómico: el pedido se guarda y el carrito se vacía en la misma transacción, o no pasa ninguna de las dos cosas.
  - Desaparece el ciclo y las dependencias van en un solo sentido, `order → cart`.
- **Negativas / deuda aceptada:**
  - `order` queda acoplado a la operación de vaciar de `cart.api`.
  - Si vaciar el carrito falla, se deshace también el pedido.
- **Cómo se verifica:**
  - `ModularityTests.modulesRespectTheirBoundaries`: `cart` ya no tiene `order :: api` en `allowedDependencies`, así que un listener de `OrderPlaced` en `cart` rompe el build.
  - En 008, un test de integración: al confirmar el pedido el carrito queda vacío en la misma transacción, y si el vaciado falla el pedido no se guarda.
