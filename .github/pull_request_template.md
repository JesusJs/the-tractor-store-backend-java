## Spec
`specs/NNN-nombre/spec.md` — resumen en una línea.

## Qué cambia
-

## Cómo verlo
```bash
./mvnw verify
curl -i http://localhost:8080/api/v1/...
```

## Checklist
- [ ] Criterios de aceptación de la spec cubiertos por tests
- [ ] `./mvnw verify` en verde (formato, unitarios, arquitectura, integración)
- [ ] Contrato respetado (`docs/api-contract.md`); sin entidades JPA expuestas
- [ ] Migraciones Flyway nuevas (no se editan las aplicadas)
- [ ] Sin secretos; configuración por variables de entorno
- [ ] `/review-spec` ejecutado; hallazgos resueltos o justificados abajo
- [ ] ADR creada si hubo decisión de arquitectura
- [ ] Entrada en `docs/practicas-ia.md`

## Hallazgos de revisión no aplicados (y por qué)
-
