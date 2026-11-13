# 0004 — Rutas /api/v1 y carrito/pedidos ligados a cookie de sesión, sin autenticación

- Estado: aceptada
- Fecha: 2026-11-12

## Contexto
El front ya implementado llama a `/api/v1/catalog/...`, `/api/v1/cart/...`, etc. La guía (B8) propone JWT con Keycloak para los pedidos, pero el reto no tiene usuarios y el plazo es corto.

## Decisión
- Todas las rutas con prefijo `/api/v1/<módulo>`.
- Cookie `ts_session` (UUID aleatorio, HttpOnly, SameSite=Lax, Secure en prod) creada por el backend. Carrito y pedidos se asocian a ella. Consultar un pedido de otra sesión devuelve 404.
- Front y API se sirven **desde el mismo origen** en Azure (el front enruta `/api/*` al backend) para que la cookie no sea de terceros. Si no fuera posible: `SameSite=None; Secure` + CORS con `allowCredentials` y origen explícito.

## Alternativas consideradas
- JWT + Keycloak: fuera de alcance; requiere login en el front.
- Id de carrito en localStorage enviado en cabecera: más simple, pero expone el id a JavaScript.

## Consecuencias
- Positivas: cero cambios en el front; sin IdP que operar.
- Negativas: quien robe la cookie ve el carrito/pedido (aceptable para una demo).
- Verificación: test de integración "pedido de otra sesión → 404".
