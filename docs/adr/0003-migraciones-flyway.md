# 0003 — Esquema y datos con Flyway; Hibernate solo valida

- Estado: aceptada
- Fecha: 2026-11-12

## Contexto
El backend .NET no tenía migraciones reales. En Azure la base es gestionada y empieza vacía.

## Decisión
Todo cambio de esquema y el dataset inicial van en migraciones Flyway versionadas (`V1__schema.sql`, `V2__dataset.sql`, ...), que se ejecutan al arrancar la app. `spring.jpa.hibernate.ddl-auto=validate` en todos los perfiles. Cada módulo es dueño de sus tablas (prefijo `catalog_`, `inventory_`, `cart_`, `order_`).

## Alternativas consideradas
- `ddl-auto=update`: no reproducible y peligroso en prod.
- Liquibase: más potente, pero más ceremonia de la necesaria.

## Consecuencias
- Positivas: BD reproducible en local, en Testcontainers y en Azure.
- Negativas: una migración aplicada no se edita; se corrige con otra nueva.
- Verificación: los tests de integración arrancan sobre un PostgreSQL vacío y migran.
