---
name: api-contract
description: Contrato HTTP del Tractor Store con el front (rutas /api/v1, modelos, formato de error, reglas de cookie de sesión). Cárgala al diseñar o implementar controllers, DTOs o tests de API.
---
# Contrato de API

La fuente de verdad es `docs/api-contract.md`. Léela completa antes de trabajar.

Reglas que más se olvidan:
- Rutas `/api/v1/<módulo>/...`; define el prefijo en una constante por módulo, no repetido a mano.
- DTOs HTTP como `record` en `infrastructure/web`, con mapeo explícito desde el dominio. Nunca devolver entidades.
- `POST /cart/items` recibe **solo** `{ sku }`; el precio se obtiene de `catalog.api`.
- `DELETE /cart/items/{sku}` elimina **solo** ese SKU.
- `recommendations` sin `skus` → 200 con lista por defecto.
- `POST /orders` → 201 + cabecera `Location`; el `id` devuelto debe funcionar en `GET /orders/{id}` con la misma cookie.
- Pedido de otra sesión → 404.
- Las líneas de pedido incluyen `sku`.
- Campos marcados ⚠️ en el contrato: pídeme confirmar contra el front antes de fijarlos.

Para cada endpoint nuevo: un `@WebMvcTest` que compruebe status, forma JSON (JSONPath) y formato de error; y documentación `@Operation` para Swagger.
