# Feature Specification: Catálogo (persistencia, errores y endpoints de catálogo)

**Feature Branch**: `002-catalogo`

**Created**: 2026-11-19

**Status**: Draft

**Input**: User description: "@docs/prompts/specify-002-catalogo.md — Funcionalidad catalogo (fusiona 002 persistencia-dataset, 004 errores-validacion y 005 catalogo del backlog): home, listados por categoría (all, classic, autonomous), detalle de producto con variantes, recomendaciones por cercanía de color, tiendas; respuestas con la forma exacta que consume el front; datos del catálogo en BD creados y poblados de forma reproducible; formato de error único {code, message, details?, traceId}. Fuente de verdad: modelos TS y servicios HTTP del front > handlers MSW > docs/api-contract.md. Regresiones .NET: recomendaciones por color y nunca por categoría; sin skus → 200 con lista por defecto; producto/categoría/SKU inexistente → 404; dataset oficial del reto con colores. Fuera de alcance: inventario, carrito, pedidos, caché."

## Clarifications

### Session 2026-11-19

- Q: El dataset oficial no trae marca ni descripción y el front las pinta; ¿de dónde salen `brand` y `description`? → A: Se derivan: `brand` = etiqueta de la categoría (`"Classic"` / `"Autonomous"`); `description` = todos los highlights del producto, en orden, unidos con `". "`.
- Q: ¿En qué puerto escucha el backend en desarrollo y qué orígenes acepta CORS? → A: Perfil `dev` en el puerto `5271`; CORS con credenciales y orígenes exactos del front, comprobados en su configuración: `http://localhost:4200` (shell, desde donde se sirve la app compuesta) y `http://localhost:4201`, `:4202`, `:4203` (remotes `mfe-explore`, `mfe-decide`, `mfe-checkout` levantados por separado, como hacen sus tests e2e). Nunca comodín.
- Q: En recomendaciones, ¿un SKU desconocido devuelve 404? → A: No. Los SKUs desconocidos se ignoran; si tras descartar vacíos y desconocidos no queda ninguno, se devuelve la lista por defecto. El endpoint nunca devuelve error de negocio. `VARIANT_NOT_FOUND` queda reservado para uso futuro (carrito).
- Q: ¿Se aprueba la ADR de FR-025 que reescribe la sección Catalog del contrato según el front? → A: Sí, aprobada.

## Contexto: el front manda, y el contrato documentado no coincide

Se revisó el front (`packages/shared-catalog`: `models/catalog.models.ts`, `lib/catalog.service.ts`,
`lib/product.service.ts`, `lib/tokens.ts`) y las pantallas que consumen esos servicios.

- **No existen handlers de MSW.** El único mock fue un interceptor HTTP propio que se vació en el
  commit `8709dc7` del front; hoy el front llama directamente a un backend real en
  `http://localhost:5271` con `withCredentials: true`. El dataset de ese mock antiguo (ids
  `tx-001`, `field-pioneer`…) **no** es el dataset oficial y no se usa como fuente.
- La forma que consume el front es la que devolvía el backend .NET anterior y **difiere en casi
  todo** de `docs/api-contract.md`. Manda el front. Diferencias:

| Endpoint | `docs/api-contract.md` dice | El front consume (manda) |
|---|---|---|
| `GET /home` | objeto `{ teasers: [{title, image, url}], strategy }` | **array** `[{ id, title, image, filter }]`; `filter` contiene `classic` o `autonomous` (el servicio envuelve el array y reordena) |
| `GET /categories/{filter}` | `{ title, filters: [{url,name,active}], products: [ProductSummary] }` | `{ category, products: [Tractor], availableFilters }`; el front sobrescribe `category` y `availableFilters`, pero usa `products` |
| Ítem de listado | `ProductSummary { id, name, image, url, startPrice }` | `Tractor { id, name, brand, price, image, variants: string[] /* SKUs */, description, enginePower?, stock? }` |
| `GET /products/{id}` | `Product { id, name, category, highlights, variants: Variant[] }` con variantes ricas (sku, name, image, color, price) | `ProductDetail { id, name, brand, price, image, description, variants: string[] /* SKUs */, highlights: string[] }` |
| `GET /recommendations` | `[Recommendation { sku, name, image, url }]` | array de `ProductItemDto { id, name, brand, price, image, variants: string[], description, enginePower?, stock }`; el front toma `variants[0]` como SKU recomendado |
| `GET /stores` | `[Store { id, name, street, city, image }]` | `[StoreLocation { id, name, address, city, image }]` (`address`, no `street`) |
| Errores | `{ code, message, details?, traceId }` | el front no lee el cuerpo del error (solo el status); el formato del contrato se mantiene |

- El front **no recibe colores**: el color de cada variante vive en el catálogo y solo se usa para
  calcular recomendaciones.
- Como `docs/api-contract.md` exige ADR para cualquier cambio, esta funcionalidad incluye la ADR que
  reescribe la sección Catalog del contrato con la forma de la tabla anterior.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Explorar el catálogo: home, listados y tiendas (Priority: P1)

Un visitante abre la tienda, ve la home con las dos categorías destacadas (clásicos y autónomos),
entra en un listado (todos, clásicos o autónomos) y ve la lista de tiendas físicas. Todo con los
datos del catálogo oficial del reto, servidos por el backend real en lugar del backend .NET.

**Why this priority**: es la primera pantalla de la demo y la puerta de entrada al resto del
recorrido; sin ella no hay demo.

**Independent Test**: con la BD vacía, se arranca el backend, se migra y se piden home, los tres
listados y las tiendas; cada respuesta tiene la forma de la tabla de contexto y los datos del
dataset oficial. Con el front sin cambios apuntando al backend, las tres pantallas se pintan.

**Acceptance Scenarios**:

1. **Given** el catálogo poblado, **When** se pide la home, **Then** se recibe una lista con un
   teaser de clásicos y otro de autónomos, cada uno con `id`, `title`, `image` y `filter`
   (`classic` / `autonomous`).
2. **Given** el catálogo poblado, **When** se pide el listado `all`, **Then** se reciben todos los
   productos del dataset, cada uno con `id`, `name`, `brand`, `price`, `image`, `variants` (SKUs) y
   `description`.
3. **Given** el catálogo poblado, **When** se pide el listado `classic` o `autonomous`, **Then**
   solo aparecen productos de esa categoría.
4. **Given** el catálogo poblado, **When** se piden las tiendas, **Then** se reciben todas las
   tiendas del dataset con `id`, `name`, `address`, `city` e `image`.
5. **Given** un filtro que no es `all`, `classic` ni `autonomous`, **When** se pide ese listado,
   **Then** se recibe 404 con el formato de error y `code = CATEGORY_NOT_FOUND`.

---

### User Story 2 - Ver el detalle de un producto (Priority: P1)

Desde un listado, el visitante abre un tractor y ve su nombre, imagen, precio, descripción,
highlights y las variantes (SKUs) que puede elegir para añadir al carrito.

**Why this priority**: el detalle es donde se elige el SKU que luego va al carrito; sin él no hay
pedido en la demo.

**Independent Test**: se pide el detalle de un producto existente y se comprueba la forma y que
`variants` contiene exactamente sus SKUs; se pide uno inexistente y se obtiene 404.

**Acceptance Scenarios**:

1. **Given** un producto del dataset, **When** se pide su detalle, **Then** se recibe `id`, `name`,
   `brand`, `price`, `image`, `description`, `variants` (todos sus SKUs, en el orden del dataset)
   y `highlights`.
2. **Given** un id que no existe, **When** se pide su detalle, **Then** se recibe 404 con el formato
   de error y `code = PRODUCT_NOT_FOUND`.

---

### User Story 3 - Recomendaciones por cercanía de color (Priority: P2)

En el detalle de un producto, el visitante ve tractores cuyo color se parece al de lo que está
mirando. En la home y en la cabecera ve una lista de recomendaciones por defecto.

**Why this priority**: es una regla de negocio distintiva del reto y una regresión del backend
anterior (recomendaba por categoría y fallaba sin `skus`), pero el pedido funciona sin ella.

**Independent Test**: se piden recomendaciones para SKUs conocidos y se comprueba que el orden
sigue la distancia de color y no la categoría; se piden sin `skus` y con `skus` vacío y se recibe
200 con la lista por defecto.

**Acceptance Scenarios**:

1. **Given** uno o varios SKUs existentes, **When** se piden recomendaciones, **Then** se reciben
   hasta 4 productos ordenados de menor a mayor distancia entre el color de su variante y el color
   promedio de los SKUs recibidos, sin incluir ninguno de los SKUs recibidos, y el SKU recomendado
   es el primero de `variants`.
2. **Given** un SKU de la categoría clásica, **When** se piden recomendaciones, **Then** el
   resultado puede incluir productos autónomos si su color es más cercano (la categoría no
   interviene).
3. **Given** que no se envía `skus`, o se envía vacío, **When** se piden recomendaciones,
   **Then** se recibe 200 con la lista por defecto, nunca un error.
4. **Given** un SKU que no existe en el catálogo, **When** se piden recomendaciones con él,
   **Then** se ignora: se calcula con los SKUs conocidos restantes o, si no queda ninguno, se
   recibe 200 con la lista por defecto; nunca un error.

---

### User Story 4 - Errores con forma única y trazables (Priority: P2)

Cuando algo falla, el front (y quien depure) recibe siempre la misma forma de error con un
identificador de traza que también aparece en los logs del backend.

**Why this priority**: es transversal a todos los módulos que vienen después; hacerlo ahora evita
rehacer cada controller.

**Independent Test**: se provocan un 404 de negocio, un 400 de validación, un 400 por petición mal
formada y un 500, y en todos se comprueba la forma del cuerpo y la cabecera `X-Trace-Id`.

**Acceptance Scenarios**:

1. **Given** cualquier error, **When** el backend responde, **Then** el cuerpo es
   `{ code, message, details?, traceId }` y la cabecera `X-Trace-Id` lleva el mismo `traceId`.
2. **Given** una petición que trae cabecera `X-Trace-Id`, **When** el backend responde (con éxito o
   error), **Then** reutiliza ese valor como `traceId`.
3. **Given** una entrada que no cumple sus reglas de validación, **When** se procesa, **Then** se
   recibe 400 `VALIDATION_ERROR` con `details` indicando el error de cada campo.
4. **Given** un fallo inesperado, **When** el backend responde, **Then** se recibe 500
   `INTERNAL_ERROR` sin stacktrace ni detalles internos en el cuerpo.

---

### User Story 5 - Catálogo reproducible en cualquier entorno (Priority: P1)

Como equipo, queremos que el catálogo se cree y se pueble solo, igual en local, en los tests y en
Azure, sin pasos manuales.

**Why this priority**: sin datos persistidos no funciona ninguna de las historias anteriores, y es
la base de las specs de inventario, carrito y pedidos.

**Independent Test**: se arranca el backend contra una BD vacía y se comprueba que el esquema y el
dataset completo quedan creados, y que un segundo arranque no duplica ni altera nada.

**Acceptance Scenarios**:

1. **Given** una BD vacía, **When** arranca el backend, **Then** se crean las tablas del catálogo y
   se carga el dataset oficial completo (productos, variantes con color y precio, highlights,
   tiendas).
2. **Given** una BD ya migrada, **When** arranca de nuevo, **Then** no se repite la carga ni cambia
   ningún dato.
3. **Given** que el modelo del código y el esquema de la BD no coinciden, **When** arranca el
   backend, **Then** el arranque falla en lugar de modificar el esquema.

---

### Edge Cases

- Filtro de categoría con mayúsculas (`Classic`) o con la etiqueta del front (`classics`): no es un
  filtro válido → 404 `CATEGORY_NOT_FOUND` (el front ya traduce `classics` → `classic`).
- `skus` con espacios o elementos vacíos (`?skus=AU-01-SI,,`): se ignoran los vacíos; si no queda
  ninguno, lista por defecto.
- `skus` con un SKU repetido: cuenta una vez para el color promedio.
- Mezcla de SKUs existentes e inexistentes: los desconocidos se ignoran y se recomienda con los
  conocidos; si todos son desconocidos, lista por defecto (200).
- Todas las variantes de un producto están entre los SKUs recibidos (el front envía todas las del
  producto que se ve): ese producto no aparece en sus propias recomendaciones.
- Dos variantes a la misma distancia de color: orden estable y determinista (por SKU).
- Varias variantes de un mismo producto entre las más cercanas: el producto aparece una sola vez,
  con su variante más cercana como recomendada.
- Ruta inexistente o método no soportado bajo `/api/v1`: responde con el formato de error único y
  el status que corresponda (404 / 405); el `code` genérico lo fija la ADR del contrato.
- Base de datos caída al pedir el catálogo: 500 `INTERNAL_ERROR` con el formato único.

## Requirements *(mandatory)*

### Functional Requirements

**Endpoints de catálogo (forma exacta que consume el front)**

- **FR-001**: `GET /api/v1/catalog/home` MUST devolver un array de teasers `{ id, title, image,
  filter }`, uno para clásicos (`filter = "classic"`) y otro para autónomos
  (`filter = "autonomous"`).
- **FR-002**: `GET /api/v1/catalog/categories/{filter}` MUST aceptar exactamente `all`, `classic` y
  `autonomous` y devolver `{ category, products, availableFilters }`, donde `category` es el filtro
  pedido, `availableFilters = ["all","classic","autonomous"]` y `products` es la lista de productos
  de esa categoría (todos para `all`) en el orden del dataset.
- **FR-003**: Cada producto de un listado MUST tener `{ id, name, brand, price, image, variants,
  description }`; `variants` es la lista de SKUs del producto; `price` es el menor precio de sus
  variantes; `image` es la imagen de su primera variante.
- **FR-004**: `GET /api/v1/catalog/products/{id}` MUST devolver `{ id, name, brand, price, image,
  description, variants, highlights }` con las mismas reglas de `price`, `image` y `variants` que
  FR-003 y los highlights del dataset.
- **FR-005**: `GET /api/v1/catalog/stores` MUST devolver un array `{ id, name, address, city,
  image }` con todas las tiendas del dataset.
- **FR-006**: `GET /api/v1/catalog/recommendations?skus=a,b` MUST devolver un array de hasta 4
  productos con la forma de FR-003, donde el primer elemento de `variants` es el SKU recomendado.
- **FR-007**: `brand` MUST ser la etiqueta de la categoría del producto (`"Classic"` para
  `classic`, `"Autonomous"` para `autonomous`) y `description` MUST ser todos sus highlights, en el
  orden del dataset, unidos con `". "` (p. ej. highlights `["A", "B"]` → `"A. B"`). Ninguno de los
  dos se guarda: se derivan al responder.
- **FR-008**: Las respuestas MUST NOT incluir `stock` ni `enginePower` (opcionales en el front y no
  usados por las pantallas); el stock es del módulo de inventario.
- **FR-009**: Todas las URLs de imagen MUST ser absolutas y cargables desde el navegador del front.

**Recomendaciones por color (regresiones .NET)**

- **FR-010**: Las recomendaciones MUST calcularse solo por cercanía de color: color promedio de
  las variantes de los SKUs recibidos, distancia euclidiana RGB de cada otra variante a ese color,
  orden ascendente, empate por SKU. La categoría MUST NOT intervenir.
- **FR-011**: MUST excluirse los SKUs recibidos y MUST aparecer cada producto una sola vez (con su
  variante más cercana).
- **FR-012**: Sin parámetro `skus`, con `skus` vacío, o cuando tras descartar vacíos y desconocidos
  no queda ningún SKU, MUST responderse
  200 con la lista por defecto: la variante primera de cada uno de los 4 primeros productos del
  dataset. Nunca error.
- **FR-013**: Los SKUs inexistentes entre los recibidos MUST ignorarse. Este endpoint MUST NOT
  devolver ningún error de negocio (solo un 500 ante un fallo inesperado).

**Errores**

- **FR-014**: Todo error MUST responder `{ code, message, details?, traceId }` y la cabecera
  `X-Trace-Id` con el mismo valor; `details` solo aparece cuando aporta información.
- **FR-015**: Si la petición trae `X-Trace-Id`, MUST reutilizarse; si no, se genera uno nuevo por
  petición. Las respuestas con éxito también MUST llevar la cabecera `X-Trace-Id`, y el
  identificador MUST aparecer en cada línea de log de esa petición.
- **FR-016**: Producto inexistente → 404 `PRODUCT_NOT_FOUND`; categoría inexistente → 404
  `CATEGORY_NOT_FOUND`. `VARIANT_NOT_FOUND` se mantiene en la jerarquía de errores para uso futuro
  (carrito); ningún endpoint de catálogo lo devuelve.
- **FR-017**: Entrada que no cumple sus reglas de validación → 400 `VALIDATION_ERROR` con
  `details` = mapa campo → mensaje; petición mal formada (cuerpo ilegible, tipo de parámetro
  incorrecto) → 400 `MALFORMED_REQUEST`.
- **FR-018**: Error no previsto → 500 `INTERNAL_ERROR`, sin stacktrace, nombres de clase ni SQL
  en el cuerpo; el detalle completo va solo al log.
- **FR-019**: Los errores de negocio MUST modelarse como una jerarquía común reutilizable por los
  módulos de inventario, carrito y pedidos, y traducirse a HTTP en un único punto compartido.

**Persistencia y dataset**

- **FR-020**: El catálogo (productos, variantes con SKU, nombre, color, precio e imagen,
  highlights, categorías y tiendas) MUST persistirse en la BD, en tablas propias del módulo de
  catálogo con su prefijo.
- **FR-021**: Esquema y dataset MUST crearse con migraciones versionadas que se aplican solas al
  arrancar, desde una BD vacía, de forma idéntica en local, tests y Azure; el esquema MUST
  validarse al arrancar y nunca generarse ni modificarse automáticamente, en ningún perfil.
- **FR-022**: El dataset MUST ser el de la especificación oficial del reto (The Tractor Store
  blueprint), con los colores de cada variante.
- **FR-023**: Los precios MUST guardarse y operarse como decimales exactos con 2 decimales.
- **FR-024**: Cada dato del catálogo MUST tener un único dueño (sin entidades duplicadas en otros
  módulos) y la lectura del detalle MUST resolverse sin una consulta por variante.

**Contrato y documentación**

- **FR-025**: Antes de implementar, una ADR (aprobada el 2026-11-19) MUST reescribir la sección Catalog de
  `docs/api-contract.md` con la forma de la tabla de contexto (incluido el `code` genérico de rutas
  inexistentes) y retirar las marcas ⚠️ resueltas.
- **FR-026**: Los 5 endpoints MUST aparecer documentados (descripción y respuestas, incluidos los
  errores) en la documentación interactiva de la API.

**Uso desde el front sin cambios**

- **FR-027**: En el perfil `dev`, el backend MUST escuchar en el puerto `5271` (el que el front
  tiene fijado en `tokens.ts`) y MUST aceptar peticiones con credenciales solo desde los orígenes
  exactos `http://localhost:4200` (shell), `http://localhost:4201`, `http://localhost:4202` y
  `http://localhost:4203` (remotes standalone). Una petición desde cualquier otro origen MUST NOT
  recibir cabeceras CORS de permiso; nunca un origen comodín.

**Guardas de arquitectura (deuda de la revisión 001)**

- **FR-028**: El build MUST fallar si: una regla de arquitectura de capas se cumple "en vacío"
  donde ya hay clases; la cara pública de un módulo expone tipos de su dominio, aplicación o
  infraestructura; una clase persistente vive fuera de la capa de persistencia de su módulo o su
  tabla no lleva el prefijo del módulo; algún perfil no valida el esquema al arrancar.

### Key Entities

- **Category**: clasificación de producto; valores `classic` y `autonomous` (más el filtro virtual
  `all`). Tiene título e imagen para el teaser de la home.
- **Product**: tractor del catálogo; id, nombre, categoría, highlights (lista ordenada), variantes. Marca y
  descripción no se guardan: se derivan de la categoría y los highlights (FR-007). Su precio de lista e imagen se derivan de sus variantes.
- **Variant**: opción comprable de un producto; SKU único, nombre, color (hex, base de las
  recomendaciones), precio y imagen. Pertenece a un único producto.
- **Color**: valor RGB de una variante; sabe calcular su distancia a otro color y el promedio de
  varios.
- **Store**: tienda física de recogida; id, nombre, dirección, ciudad, imagen.
- **ApiError**: forma única de error (`code`, `message`, `details?`, `traceId`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con el front sin modificar apuntando al backend en local, las pantallas de home, los
  3 listados, el detalle de producto, las recomendaciones y las tiendas se pintan con datos, sin
  errores de red ni de consola causados por la forma de las respuestas.
- **SC-002**: Tras arrancar contra una BD vacía, el 100 % de los productos, variantes, highlights y
  tiendas del dataset oficial están disponibles, y un segundo arranque deja exactamente los mismos
  datos.
- **SC-003**: En 100 peticiones de recomendaciones sin `skus` o con `skus` vacío, el 100 %
  responde con éxito y la misma lista por defecto.
- **SC-004**: Para un conjunto de casos de prueba de colores conocido, el 100 % de las
  recomendaciones coincide con el orden por distancia de color esperado, incluidos casos donde el
  más cercano es de otra categoría.
- **SC-005**: El 100 % de las respuestas de error de los escenarios de esta spec tienen la forma
  única y su `traceId` coincide con la cabecera `X-Trace-Id`.
- **SC-006**: Cada pantalla del catálogo recibe su respuesta en menos de 1 segundo en local con el
  dataset completo.

## Assumptions

- **Fuente del contrato**: no hay handlers de MSW en el front; se usan los modelos TS y los
  servicios HTTP (fuente 1). El mock vaciado en el commit `8709dc7` no es el dataset oficial.
- **Dataset oficial**: el del repositorio público de The Tractor Store blueprint (productos `CL-xx`
  y `AU-xx`, SKUs tipo `AU-01-SI`, variantes con color, precios, highlights y tiendas); la
  transcripción exacta y las URLs absolutas de sus imágenes se fijan en el plan.
- **Precio e imagen del producto**: el front muestra un único precio por producto; se usa el menor
  precio entre sus variantes ("desde") y la imagen de la primera variante.
- **Recomendaciones**: N = 4 (lo que marcaba el contrato con ⚠️); un producto por recomendación; el
  SKU recomendado va primero en `variants` porque el front usa `variants[0]`.
- **SKU inexistente**: en recomendaciones, los SKUs desconocidos se ignoran (decisión del
  2026-11-19, que sustituye la regla "SKU inexistente → 404" del input para este endpoint); la
  regla "SKU inexistente → 404" aplicará al carrito con `VARIANT_NOT_FOUND`.
- **Precios por variante (a verificar en el plan)**: el plan MUST comprobar si en el dataset
  oficial las variantes de un mismo producto tienen precios distintos. Si es así, se documenta como
  limitación conocida: el front muestra un único precio "desde" por producto y el carrito usará el
  precio real de la variante elegida, así que el importe del carrito puede ser mayor que el mostrado
  en el listado o el detalle.
- **Teasers de la home**: `title` e `image` vienen del dataset; el front sobrescribe el título y
  reordena, así que solo `filter` es crítico.
- **CORS y puerto**: aunque el backlog situaba CORS en 007, el front ya envía `withCredentials` en
  las llamadas de catálogo, así que se adelanta aquí; 007 lo reutiliza. Puertos del front tomados de
  los `project.json` de Nx (shell sin `port` explícito → 4200 por defecto, `publicHost`
  `http://localhost:4200`). El puerto 5271 aplica solo al perfil `dev`; los orígenes de Azure se
  configuran por variable de entorno en la spec de despliegue.
- **Fuera de alcance**: inventario (y el campo `stock`), carrito, pedidos, caché.
- **Dependencias**: esqueleto de 001 (módulos, Flyway sin tablas, Testcontainers, ArchUnit,
  Swagger); constitución v1.0.0.
