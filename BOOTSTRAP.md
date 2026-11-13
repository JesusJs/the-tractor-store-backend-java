# Arranque — día 1 (desde Claude Desktop, pestaña Code)

Repo: https://github.com/JesusJs/the-tractor-store-backend-java (vacío). Carpeta local ya creada.
Tiempo estimado: 45–60 min. Cada paso termina con algo que puedes **ver**.

## Paso 0 — Prerrequisitos (terminal de Kali)
```bash
java -version          # 21.x   (si no: sdk install java 21.0.x-tem)
docker version         # Docker Desktop corriendo
gh auth status         # sesión iniciada en GitHub
uv --version           # si no: curl -LsSf https://astral.sh/uv/install.sh | sh
```
✅ Ves las cuatro versiones.

## Paso 1 — Conectar la carpeta con el repo
En la carpeta que ya creaste:
```bash
git init -b main
git remote add origin https://github.com/JesusJs/the-tractor-store-backend-java.git
```
(Si la clonaste con `gh repo clone`, sáltate este paso.)

## Paso 2 — Instalar e inicializar Spec Kit
```bash
uv tool install specify-cli
specify version                                   # 1.2.x
specify init --here --integration claude --script sh
```
✅ Aparecen `.specify/` (plantillas, scripts, `memory/constitution.md` plantilla) y `.claude/skills/speckit-*/`.

> Si `specify init` te pregunta por sobrescribir porque la carpeta no está vacía, responde que sí: aún no hay nada tuyo.

## Paso 3 — Copiar este kit encima
Descomprime `tractor-store-backend-kit.zip` **en la raíz del repo** (sin sobrescribir lo de Spec Kit):
```bash
unzip -n ~/Descargas/tractor-store-backend-kit.zip -d .
chmod +x .claude/hooks/format-java.sh
tree -a -L 3 -I .git .
```
✅ Ves `.claude/skills/` con las de Spec Kit **y** las tuyas (review-spec, pr, adr…), `.claude/agents/` con 3 revisores, `docs/` con contrato, backlog, ADRs y prompts.

Opcional (vale para todos tus repos): en `~/.claude/settings.json` añade `"attribution": {"commit": "", "pr": ""}`. El `.claude/settings.json` del repo ya lo trae.

## Paso 4 — Primer commit
```bash
git add -A && git commit -m "chore: bootstrap spec kit and AI development kit"
git push -u origin main
```
✅ Lo ves en GitHub.

## Paso 5 — Abrir Claude Desktop → Code → seleccionar la carpeta
Comprobaciones dentro de la sesión:
- `/agents` → ves `spec-reviewer`, `architecture-reviewer`, `security-reviewer`.
- Escribe `/` → ves `/speckit-*` y `/review-spec`, `/pr`, `/adr`, `/log-practica`, `/document`.
- `/hooks` → ves el PostToolUse de formato.

## Paso 6 — Constitución (tu primer uso de Spec Kit)
```
/speckit-constitution @docs/prompts/constitution-input.md
```
Lee `.specify/memory/constitution.md` con calma: es el documento que el agente obedecerá en cada plan. Pide cambios si algo no te representa. Luego:
```
commit de la constitución  →  /log-practica
```
🎓 Concepto: la constitución es contexto persistente de "reglas no negociables"; `/speckit-plan` la contrasta en su *Constitution Check*.

## Paso 7 — Spec 001 (esqueleto) con el ciclo completo
```
git switch -c 001-esqueleto-modular          # si Spec Kit no crea la rama solo
/speckit-specify @docs/prompts/specify-001.md
/speckit-clarify                              # responde sus preguntas
   → revisa specs/001-*/spec.md y apruébala
/clear
/speckit-plan @docs/prompts/plan-001.md
/speckit-tasks
/speckit-analyze                              # consistencia spec ↔ plan ↔ tareas ↔ constitución
   → revisa plan.md y tasks.md
/clear
/speckit-implement
/review-spec
/pr
/log-practica
```
✅ Final del día 2: PR verde con `./mvnw verify`, `/actuator/health` = UP y un test de arquitectura que falla si rompes una frontera.

## Si algo no encaja
- Nombres de comandos de Spec Kit distintos → `specify --help` y mira `.claude/skills/`.
- Claude genera código de Boot 3 → recuérdale cargar la skill `spring-boot-4-notes`.
- Se alarga una fase → `/clear` y retoma desde los archivos de `specs/NNN/`, no desde la conversación.
