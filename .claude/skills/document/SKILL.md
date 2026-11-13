---
name: document
description: Actualiza README, ARCHITECTURE, TESTING y DEPLOYMENT tras cerrar una spec, sin inventar nada que el código no haga.
disable-model-invocation: true
---
# /document

1. Lee el diff de la spec y los documentos existentes.
2. Actualiza solo lo afectado:
   - `README.md`: qué es, cómo correrlo en 5 minutos, enlace a Swagger UI, URL desplegada.
   - `docs/ARCHITECTURE.md`: diagrama de módulos (usa el que genera Spring Modulith en `target/spring-modulith-docs` si existe) y eventos.
   - `docs/TESTING.md`: tipos de test, herramientas, umbrales y exclusiones.
   - `docs/DEPLOYMENT.md`: infraestructura, pipeline y rollback.
3. Cada comando que documentes debe haberse ejecutado al menos una vez; si no, márcalo `(sin verificar)`.
