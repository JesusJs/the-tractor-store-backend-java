Funcionalidad: **catalogo** (fusiona las specs 002 persistencia, 004 errores y 005 catálogo del backlog).

Qué y por qué:
- Como visitante quiero ver la home, los listados por categoría (todas, classic, autonomous), el
  detalle de un producto con sus variantes, recomendaciones según el color de los productos que
  estoy viendo y la lista de tiendas, para explorar y decidir qué tractor comprar.
- Como frontend (ya implementado) necesito que cada respuesta tenga exactamente la forma que
  consumo hoy, para cambiar los mocks por el backend real sin tocar código.
- Como equipo queremos que los datos del catálogo vivan en la base de datos, creados y poblados
  de forma reproducible, para que funcione igual en local, en los tests y en Azure.
- Como frontend necesito errores con una forma única ({code, message, details?, traceId}).

Fuente de verdad del contrato (en este orden):
1. Los modelos TypeScript y los servicios HTTP del front (directorio añadido con /add-dir).
2. Los handlers de MSW del front: forma del JSON y dataset.
3. docs/api-contract.md: si discrepa con el front, manda el front; lista las diferencias y
   actualiza el contrato.

Reglas de negocio (regresiones del backend .NET):
- Recomendaciones por cercanía de color, nunca por categoría.
- Recomendaciones sin skus o con skus vacíos → 200 con lista por defecto, nunca error.
- Producto, categoría o SKU inexistente → 404 con el formato de error.
- Dataset de la especificación oficial del reto, con los colores de las variantes.

Fuera de alcance: inventario, carrito, pedidos, caché.
