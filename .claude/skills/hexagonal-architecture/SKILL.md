---
name: hexagonal-architecture
description: Cómo organizar cada módulo en capas hexagonales (domain, application, infrastructure, api) y en qué orden implementar. Cárgala al crear o mover clases dentro de un módulo.
---
# Arquitectura hexagonal dentro de cada módulo

```
com.tractorstore.cart
├── package-info.java                 @ApplicationModule
├── api/                              @NamedInterface("api") — cara pública
│   ├── CartApi.java                  interfaz que usan otros módulos
│   └── CartSnapshot.java             record de intercambio
├── domain/                           JAVA PURO
│   ├── Cart.java, LineItem.java      agregado y valores (invariantes aquí)
│   ├── CartNotFound... excepciones   extienden una BusinessException común (shared)
│   └── port/CartRepository.java      puerto de salida (interfaz)
├── application/
│   ├── AddItemToCart.java            puerto de entrada (interfaz del caso de uso)
│   ├── AddItemToCartService.java     implementación; @Service @Transactional
│   └── CartApiAdapter.java           implementa api.CartApi delegando en casos de uso
└── infrastructure/
    ├── web/CartController.java       adaptador de entrada → solo casos de uso
    ├── web/dto/AddItemRequest.java   record + Bean Validation
    ├── persistence/CartEntity.java   @Entity (tabla cart_*)
    ├── persistence/JpaCartRepository.java  extends JpaRepository
    └── persistence/CartRepositoryAdapter.java implementa domain.port.CartRepository
```

Reglas (las verifica ArchUnit):
- `domain` no importa `org.springframework..`, `jakarta.persistence..`, `com.fasterxml..`, `tools.jackson..`.
- `application` no importa `infrastructure`.
- Controllers no dependen de `persistence` ni de `JpaRepository`.
- La entidad JPA y el modelo de dominio son clases distintas; el adaptador mapea.

Pragmatismo acordado: en `application` se permiten `@Service` y `@Transactional`; nada más de Spring.

Orden de implementación (inside-out, test primero):
1. Test unitario del dominio → dominio.
2. Test unitario del caso de uso con el puerto mockeado (Mockito) → caso de uso.
3. Test de integración del adaptador de persistencia (Testcontainers) → adaptador + migración.
4. `@WebMvcTest` del controller con el caso de uso mockeado → controller + DTOs.
5. Un test de integración del endpoint de punta a punta.
