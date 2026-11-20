# 0001 — Monolito modular con arquitectura hexagonal por módulo

- Estado: aceptada
- Fecha: 2026-11-12

## Contexto
El front está dividido en micro-frontends, pero el backend lo opera una sola persona y hay 9 días hasta la demo. El backend .NET anterior tenía entidades duplicadas y controllers que accedían a repositorios directamente.

## Decisión
Un solo despliegue Spring Boot con los módulos `catalog`, `inventory`, `cart`, `order` y `notifications`. Dentro de cada módulo, capas hexagonales: `domain` (Java puro), `application` (casos de uso), `infrastructure` (adaptadores web y JPA) y `api` (única cara pública). Fronteras verificadas con Spring Modulith `verify()` y ArchUnit en cada build.

## Alternativas consideradas
- Microservicios: N despliegues, N bases y red entre servicios; coste operativo desproporcionado.
- Monolito por capas (`controllers/`, `services/`): sin fronteras; fue el origen de los problemas en .NET.
- Solo `api`/`internal` (como la guía, B9): válido, pero no separa el dominio del framework; la hexagonal además es objetivo de aprendizaje.

## Consecuencias
- Positivas: fronteras explícitas y testeables; extraer un módulo a servicio sería mecánico.
- Negativas: más paquetes y mapeos (entidad ↔ dominio ↔ DTO).
- Verificación: `ModularityTests` (Modulith) y `HexagonalArchitectureTests` (ArchUnit) en `./mvnw test`.

## Actualización 2026-11-20
La [ADR 0009](0009-modulo-shared-errores-web.md) añade el módulo técnico abierto `shared`
(jerarquía de errores, `traceId`, CORS). Quedan cinco módulos de dominio más `shared`, que no tiene
entidades ni tablas y del que los dominios solo usan `shared.error` (Java puro). La decisión
original de este documento no cambia.
