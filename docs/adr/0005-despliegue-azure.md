# 0005 — Despliegue en Azure Container Apps con Terraform y GitHub Actions (OIDC)

- Estado: aceptada
- Fecha: 2026-11-12

## Contexto
Objetivo personal: rol DevOps/Cloud en Azure. La guía (C5) sugiere Fly.io/Render, y menciona Azure como opción natural en contexto empresarial.

## Decisión
- Imagen Docker multi-stage → Azure Container Registry.
- Azure Container Apps (escala a 0–1 réplicas) + Azure Database for PostgreSQL Flexible Server (SKU burstable más pequeño) + Log Analytics.
- Infraestructura en Terraform (`infra/`), estado remoto en Storage Account.
- GitHub Actions se autentica con **OIDC** (federated credential), sin secretos de larga duración. Credenciales de BD como secret de Container Apps.

## Alternativas consideradas
- App Service: válido, pero Container Apps es más cercano al trabajo con contenedores.
- AKS: excesivo para un monolito.

## Consecuencias
- Positivas: práctica real de IaC y CI/CD en Azure; URL HTTPS gratis.
- Negativas: coste de PostgreSQL Flexible (apagar o destruir tras la demo); arranque en frío si escala a 0.
- Verificación: smoke test `GET /actuator/health` tras cada deploy.
