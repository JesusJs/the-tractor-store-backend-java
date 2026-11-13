#!/usr/bin/env bash
# PostToolUse: formatea con Spotless el .java recién editado. Nunca bloquea a Claude.
set -u
input="$(cat)"
regex="$(printf '%s' "$input" | python3 -c '
import json, re, sys
try:
    p = json.load(sys.stdin).get("tool_input", {}).get("file_path", "")
except Exception:
    p = ""
print(re.escape(p) if p.endswith(".java") else "")
' 2>/dev/null)"
[ -n "$regex" ] || exit 0
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
[ -x ./mvnw ] || exit 0          # todavía no existe el proyecto Maven
if ! ./mvnw -q -o spotless:apply -DspotlessFiles="$regex" >/dev/null 2>&1; then
  echo "format-java: Spotless no pudo formatear el archivo (¿plugin aún no configurado?)" >&2
fi
exit 0
