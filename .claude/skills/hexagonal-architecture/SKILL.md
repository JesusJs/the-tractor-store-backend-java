---
name: hexagonal-architecture
description: Cómo organizar cada módulo en capas hexagonales (domain, application, infrastructure, api) y en qué orden implementar. Cárgala al crear o mover clases dentro de un módulo.
---
# Arquitectura hexagonal dentro de cada módulo

**Estructura ESTÁNDAR para todos los módulos** (`catalog` ya la sigue; `cart`, `order`, `inventory` y
`notifications` la seguirán). Ejemplo con `cart`:

```
com.tractorstore.cart
├── package-info.java                       @ApplicationModule
├── api/                                    @NamedInterface("api") — cara pública
│   ├── CartApi.java                        interfaz que usan otros módulos
│   └── CartSnapshot.java                   record de intercambio
├── domain/                                 JAVA PURO
│   ├── model/                              agregados, entidades y valores (invariantes aquí)
│   │   ├── Cart.java, LineItem.java
│   │   └── DomainChecks.java               helpers de invariantes, package-private
│   ├── service/                            políticas/reglas de dominio sin estado (p. ej. ColorRecommendationPolicy)
│   ├── exception/                          CartNotFoundException… extienden BusinessException (shared)
│   └── port/out/                           puertos de salida (interfaces *Repository, gateways)
│       └── CartRepository.java
├── application/
│   ├── port/in/                            puertos de entrada: una interfaz *UseCase por caso de uso
│   │   └── AddItemToCartUseCase.java
│   ├── service/                            implementaciones *Service (@Service @Transactional), package-private
│   │   ├── AddItemToCartService.java       implements AddItemToCartUseCase
│   │   └── CartApiAdapter.java             implementa api.CartApi delegando en casos de uso
│   └── model/                              modelos de lectura/escritura propios de la aplicación (records)
└── infrastructure/
    ├── web/
    │   ├── controller/                     CartController (→ solo *UseCase), CartPaths
    │   ├── dto/                            *Request (record + Bean Validation), *Response
    │   └── mapper/                         CartDtoMapper: dominio ↔ DTO HTTP
    ├── persistence/
    │   ├── entity/                         *Entity (@Entity, tabla cart_*), públicas para adapter/
    │   ├── repository/                     Jpa*Repository extends JpaRepository, públicas para adapter/
    │   └── adapter/                        *RepositoryAdapter implementa domain.port.out.*Repository
    └── config/                             wiring de Spring si hace falta
```

Nombres:
- Puerto de entrada `XxxUseCase`; su implementación `XxxService` (mismo prefijo).
- Puerto de salida `XxxRepository` (dominio); su adaptador `XxxRepositoryAdapter`; el Spring Data `JpaXxxRepository`.
- Los tests viven en el paquete equivalente al de la clase que prueban (así ven lo package-private).

Visibilidad: package-private por defecto; `public` solo cuando otro subpaquete lo necesita
(p. ej. entidades y `Jpa*Repository` los usa `adapter/`; los `*Service` siguen package-private porque
Spring los inyecta por su `*UseCase`).

Reglas (las verifica ArchUnit en `HexagonalArchitectureTests`):
- `domain` no importa `org.springframework..`, `jakarta.persistence..`, `com.fasterxml..`, `tools.jackson..`.
- `domain` y `application` no importan `infrastructure`.
- Controllers no dependen de `persistence` ni de `JpaRepository`.
- Spring Data y `@Entity` solo en `infrastructure.persistence`.
- Interfaces de `application` solo en `application.port.in`.
- Clases `*Repository` de `domain` solo en `domain.port.out`.
- La entidad JPA y el modelo de dominio son clases distintas; el adaptador mapea.

Pragmatismo acordado: en `application` se permiten `@Service` y `@Transactional`; nada más de Spring.

Orden de implementación (inside-out, test primero):
1. Test unitario del dominio → `domain/model`, `domain/service`, `domain/exception`.
2. Test unitario del caso de uso con el puerto mockeado (Mockito) → `application/port/in` + `application/service`.
3. Test de integración del adaptador de persistencia (Testcontainers) → `persistence/{entity,repository,adapter}` + migración.
4. `@WebMvcTest` del controller con el caso de uso mockeado → `web/{controller,dto,mapper}`.
5. Un test de integración del endpoint de punta a punta.
