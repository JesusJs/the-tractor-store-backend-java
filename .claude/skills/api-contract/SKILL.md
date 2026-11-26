---
name: api-contract
description: Contrato HTTP del Tractor Store con el front (rutas /api/v1, modelos, formato de error, reglas de cookie de sesión). Cárgala al diseñar o implementar controllers, DTOs o tests de API.
---
# Contrato de API

La fuente de verdad es `docs/api-contract.md`. Léela completa antes de trabajar.

Reglas que más se olvidan:
- Rutas `/api/v1/<módulo>/...`; define el prefijo en una constante por módulo, no repetido a mano.
- DTOs HTTP como `record` en `infrastructure/web`, con mapeo explícito desde el dominio. Nunca devolver entidades.
- No hay carrito en servidor: `GET /cart` solo fija la cookie y devuelve un `Cart` vacío (ADR 0011).
- `POST /orders` usa de cada línea solo `variantId` (SKU) y `quantity`; precio, nombres e imagen salen de `catalog.api`. IVA 21 % `HALF_UP` en el backend.
- `storeId` se valida contra las tiendas del catálogo (`catalog.api`) → 404 `STORE_NOT_FOUND` si no existe (enmienda ADR 0011).
- `extraPickups` llega como string `"a, b"` y el recibo lo devuelve como `string[]` (nunca `null`).
- `recommendations` sin `skus` → 200 con lista por defecto.
- `POST /orders` → 201 + cabecera `Location`; el `id` devuelto debe funcionar en `GET /orders/{id}` con la misma cookie.
- Pedido de otra sesión → 404.
- Las líneas del recibo incluyen `variantId` = SKU.
- Si el front y el contrato discrepan, manda el front: pídeme confirmar y propón ADR (como las 0008 y 0011).

Para cada endpoint nuevo: un `@WebMvcTest` que compruebe status, forma JSON (JSONPath) y formato de error; y documentación `@Operation` para Swagger.
