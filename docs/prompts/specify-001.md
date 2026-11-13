Funcionalidad: **esqueleto-modular** del backend del Tractor Store.

Qué y por qué (sin detalles de implementación, eso va en el plan):
- Como desarrollador quiero un proyecto que arranque, compile y pase sus tests desde el primer día, con los cinco módulos del dominio (catalog, inventory, cart, order, notifications) ya separados, para que todo lo que se añada después respete las fronteras.
- Como operador quiero un endpoint de salud que diga si la aplicación y su base de datos están vivas, para usarlo en las probes de Azure.
- Como equipo queremos que el build falle si: un módulo usa el interior de otro, el dominio depende del framework, un controller usa un repositorio, o el código no está formateado.
- Como desarrollador quiero levantar la base de datos local con un solo comando.
- Como equipo queremos un pipeline de CI que ejecute el build y los tests en cada push y PR.

Criterios de aceptación esperados (ajústalos en la spec):
1. `./mvnw verify` termina en verde en una máquina limpia con JDK 21 y Docker.
2. Existe un test que falla si el módulo `cart` importa una clase interna de `catalog` (se demuestra rompiéndolo a propósito y revirtiendo).
3. `GET /actuator/health` devuelve `UP` con la BD levantada en docker compose.
4. El pipeline de GitHub Actions corre en el PR de esta rama y está en verde.
5. No hay ningún endpoint de negocio todavía.

Fuera de alcance: persistencia de negocio, Flyway con datos, despliegue a Azure.
