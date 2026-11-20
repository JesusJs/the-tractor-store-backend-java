# Quickstart — validar 002 catálogo

Guía para comprobar de punta a punta que la funcionalidad cumple la spec. Formas exactas en
[contracts/catalog-api.md](contracts/catalog-api.md) y [contracts/errors.md](contracts/errors.md);
tablas y dataset en [data-model.md](data-model.md).

## Prerrequisitos

- Java 21, Docker en marcha, `jq` y `curl`.
- Rama `002-catalogo`. Para §6, el repo del front con `pnpm install` hecho.

## 1. Pirámide de tests

```bash
./mvnw test
```

Esperado: verde. Incluye `ColorTest`, `ProductTest`, `ColorRecommendationPolicyTest` (regresiones
.NET), `*ServiceTest`, `CatalogControllerTest`, `GlobalExceptionHandlerTest`, `TraceIdFilterTest`,
`ModularityTests`, `HexagonalArchitectureTests` (sin `allowEmptyShould` salvo la regla de `api`),
`ArchitectureRulesSelfTests` y `JpaSchemaValidationTests`.

```bash
./mvnw verify
```

Esperado: verde. Añade `CatalogPersistenceIT` (23/47/69/4 filas, orden, 1 sentencia SQL en el
detalle, segunda migración sin cambios), `CorsIT`, `CatalogApiIT` y `ApplicationIT`.

## 2. Arrancar contra una BD vacía (US5)

```bash
docker compose down -v
```

```bash
docker compose up -d postgres
```

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Esperado en el log: Flyway aplica `2 - catalog schema` y `3 - catalog dataset`; el servidor escucha
en el **5271**; cada línea de log de una petición lleva `[<traceId>]`.

Recuento en la BD:

```bash
docker compose exec postgres psql -U tractor -d tractor -c "select (select count(*) from catalog_product) p, (select count(*) from catalog_variant) v, (select count(*) from catalog_product_highlight) h, (select count(*) from catalog_store) s"
```

Esperado: `23 | 47 | 69 | 4`. Parar y volver a arrancar la app: el log dice que el esquema está al
día y los recuentos no cambian (US5-2).

## 3. Endpoints (US1, US2, US3)

```bash
curl -s localhost:5271/api/v1/catalog/home | jq
```

Esperado: 2 teasers con `filter` `classic` y `autonomous` e imágenes absolutas.

```bash
curl -s localhost:5271/api/v1/catalog/categories/all | jq '.products | length'
```

Esperado: `23` (y `15` para `classic`, `8` para `autonomous`).

```bash
curl -s localhost:5271/api/v1/catalog/products/AU-07 | jq
```

Esperado: el ejemplo de [catalog-api.md](contracts/catalog-api.md#get-apiv1catalogproductsid).

```bash
curl -s "localhost:5271/api/v1/catalog/recommendations?skus=AU-01-SI" | jq '[.[] | .variants[0]]'
```

Esperado: `["AU-07-MT","CL-15-SD","CL-08-GR","CL-11-SK"]` (cruza categorías: regresión .NET).

```bash
curl -s "localhost:5271/api/v1/catalog/recommendations" | jq '[.[] | .variants[0]]'
```

Esperado: `["CL-01-GR","CL-02-BL","CL-03-GR","CL-04-BL"]`; lo mismo con `?skus=`, `?skus=,,` y
`?skus=NOPE-00`. Nunca 4xx.

```bash
curl -s localhost:5271/api/v1/catalog/stores | jq 'length'
```

Esperado: `4`.

Comprobar que cada URL de imagen carga (FR-009):

```bash
curl -s localhost:5271/api/v1/catalog/categories/all | jq -r '.products[].image' | xargs -n1 curl -s -o /dev/null -w '%{http_code}\n' | sort | uniq -c
```

Esperado: `23 200`.

## 4. Errores y traza (US4)

```bash
curl -si localhost:5271/api/v1/catalog/products/CL-99
```

Esperado: `404`, cabecera `X-Trace-Id`, cuerpo `{"code":"PRODUCT_NOT_FOUND",...,"traceId":"<mismo>"}`
sin `details`.

```bash
curl -si -H 'X-Trace-Id: demo-trace-1' localhost:5271/api/v1/catalog/categories/classics
```

Esperado: `404 CATEGORY_NOT_FOUND`, `X-Trace-Id: demo-trace-1` y `"traceId":"demo-trace-1"`. En el
log del backend aparece `[demo-trace-1]`.

```bash
curl -si "localhost:5271/api/v1/catalog/products/$(printf 'X%.0s' {1..30})"
```

Esperado: `400 VALIDATION_ERROR` con `details.id`.

```bash
curl -si -X POST localhost:5271/api/v1/catalog/home
```

Esperado: `405 METHOD_NOT_ALLOWED` con el formato único. `GET /api/v1/catalog/products` → `404
NOT_FOUND`.

El 500 (`INTERNAL_ERROR`, sin stacktrace) se comprueba en `GlobalExceptionHandlerTest`; a mano:
`docker compose stop postgres` y repetir cualquier GET de catálogo.

## 5. CORS (FR-027)

```bash
curl -si -H 'Origin: http://localhost:4201' localhost:5271/api/v1/catalog/home | grep -i '^access-control'
```

Esperado: `Access-Control-Allow-Origin: http://localhost:4201`,
`Access-Control-Allow-Credentials: true`, `Access-Control-Expose-Headers` con `X-Trace-Id`.

```bash
curl -si -H 'Origin: http://evil.example' localhost:5271/api/v1/catalog/home | head -1
```

Esperado: `403` y ninguna cabecera `Access-Control-Allow-Origin`.

Arrancar con `CORS_ALLOWED_ORIGINS='*'` debe fallar en el arranque.

## 6. Con el front real sin cambios (SC-001)

Con el backend del §2 en marcha, en el repo del front:

```bash
pnpm nx serve shell
```

Abrir `http://localhost:4200` y recorrer: home (2 categorías), Classics / Autonomous / All, detalle
de un producto (variantes y highlights), recomendaciones en el detalle y en la home, y la lista de
tiendas en checkout. Esperado: todo se pinta con imágenes del CDN, sin errores de red ni de CORS en
la consola. Evidencia (capturas) para el PR.

## 7. Swagger (FR-026)

Abrir `http://localhost:5271/swagger-ui.html`: tag `Catalog` con los 5 endpoints, cada uno con
descripción y respuestas 200 y de error con el esquema `ApiError`.
