# 0006 — Spring Boot 4.1 en vez del 3.x de la guía

- Estado: aceptada
- Fecha: 2026-11-12

## Contexto
La guía indica Spring Boot 3.x. Spring Boot 3.5 dejó de tener soporte OSS el 30-jun-2026; la línea estable es 4.1.x (4.2 prevista para la semana del 19-nov-2026).

## Decisión
Spring Boot 4.1.x (último parche) + Spring Modulith 2.x + Java 21. No se migra a 4.2 antes de la demo.

## Alternativas consideradas
- 3.5.x: más ejemplos en internet y la IA lo conoce mejor, pero ya no recibe parches OSS.

## Consecuencias
- Positivas: versión soportada; Modulith 2 con el registro de eventos renovado.
- Negativas: cambios respecto a Boot 3 (starters modulares, Jackson 3, paquetes de test slices, Testcontainers 2). La IA puede generar código de Boot 3 → skill `spring-boot-4-notes` y compilar/testear siempre.
- Verificación: build verde en CI.
