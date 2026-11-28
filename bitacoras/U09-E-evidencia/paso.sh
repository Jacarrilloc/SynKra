#!/usr/bin/env bash
# U09-E, decisión 4: deja la evidencia de un paso del recorrido de la demo por el front. Solo lee.
# Uso (desde la raíz del worktree o desde cualquier sitio): bash bitacoras/U09-E-evidencia/paso.sh <n> <nombre>
#   <n> = 00 a 11 (dos cifras); <nombre> = el de la tabla (inicio, vista, catalogo, cu2-verifica, cu2-plan, cu2-confirma,
#   cu2-listo, cu4-rechazo, cu5-fallo, cu5-fallido, cu5-vuelta, cu5-listo).
# Escribe aidlc-docs/construction/U09-front/code/evidencia/recorrido/paso-<n>-<nombre>.txt y actualiza .marca (la hora de este paso).
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$RAIZ"
N="${1:-}"; NOMBRE="${2:-}"
NOMBRES=(inicio vista catalogo cu2-verifica cu2-plan cu2-confirma cu2-listo cu4-rechazo cu5-fallo cu5-fallido cu5-vuelta cu5-listo)
if ! [[ "$N" =~ ^(0[0-9]|1[01])$ ]] || [ "${NOMBRES[$((10#$N))]}" != "$NOMBRE" ]; then
  echo "uso: bash bitacoras/U09-E-evidencia/paso.sh <n> <nombre>   (n=00..11; nombre de la tabla: ${NOMBRES[*]})" >&2
  exit 2
fi

KC="${KUBECONFIG_SYNKRA:-$RAIZ/.kube/kind-synkra.yaml}"
CRT="$RAIZ/.tls/tls.crt"
DIR="$RAIZ/aidlc-docs/construction/U09-front/code/evidencia/recorrido"
OUT="$DIR/paso-$N-$NOMBRE.txt"
MARCA="$DIR/.marca"
mkdir -p "$DIR"
ERR="$(mktemp)"; trap 'rm -f "$ERR"' EXIT

AHORA="$(date -u +%Y-%m-%dT%H:%M:%S.%NZ)"   # la marca de este paso: se guarda al final
: > "$OUT"

# cap <orden...>: añade su salida al archivo; si falla, deja la línea FALLA y sale con 1.
cap() {
  local rc=0 o
  o="$("$@" 2>"$ERR")" || rc=$?
  if [ "$rc" -ne 0 ]; then
    echo "FALLA paso $N: $* salió con $rc" >> "$OUT"
    head -c 300 "$ERR" >> "$OUT" || true
    exit 1
  fi
  CAP="$o"
}
kc() { kubectl --kubeconfig "$KC" --request-timeout=60s "$@"; }
api() { curl -sS -f --max-time 60 --resolve synkra.local:443:127.0.0.1 --cacert "$CRT" "https://synkra.local$1"; }

cap kc config current-context
[ "$CAP" = kind-synkra ] || { echo "FALLA paso $N: el contexto es '$CAP', no kind-synkra" >> "$OUT"; exit 1; }
VC="$(sed -n 's/^version = "\(.*\)"$/\1/p' core/build.gradle.kts)"

{
  echo "fecha: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "sha: $(git rev-parse HEAD)"
  echo "paso: $N $NOMBRE"
  echo "VC=$VC (versión del core, de core/build.gradle.kts)"
} >> "$OUT"

# --- 1. el log de la API visto desde el front
if [ "$N" != 00 ] && [ -s "$MARCA" ]; then DESDE="$(cat "$MARCA")"; else DESDE=""; fi
echo "" >> "$OUT"
echo "## log del front (System.Net.Http.HttpClient.ApiSynkra), desde ${DESDE:-el arranque del pod}" >> "$OUT"
if [ -n "$DESDE" ]; then cap kc logs -n synkra deploy/synkra-front --since-time="$DESDE"
else cap kc logs -n synkra deploy/synkra-front; fi
LOG="$CAP"
FILTRADO="$(printf '%s\n' "$LOG" | awk '
  /System\.Net\.Http\.HttpClient\.ApiSynkra\./ { cat=1; next }
  cat==1 { if ($0 ~ /Sending HTTP request|Received HTTP response/) print; cat=0 }')"
[ -z "$FILTRADO" ] || printf '%s\n' "$FILTRADO" >> "$OUT"
echo "" >> "$OUT"
echo "## llamadas (MÉTODO ruta código; las rutas con consulta pueden salir redactadas como ?*)" >> "$OUT"
LISTA="$(printf '%s\n' "$FILTRADO" | awk '
  /Sending HTTP request/ { m=$4; u=$5; sub(/^https?:\/\/[^\/]*/, "", u); cola[++t]=m " " u; next }
  /Received HTTP response/ { print cola[++h] " " $NF }')"
[ -z "$LISTA" ] || printf '%s\n' "$LISTA" >> "$OUT"
[ -n "$LISTA" ] || echo "(ninguna)" >> "$OUT"

# --- 2. la lectura de vuelta de la API
cap api /api/paises; PAISES="$CAP"
cap api /api/paises/pe/despliegues; DESP="$CAP"
echo "" >> "$OUT"
echo "## API: GET /api/paises" >> "$OUT"
cap jq -c '[.[] | {pais, lectura, concuerda, ultimoDespliegueId}]' <<<"$PAISES"
echo "$CAP" >> "$OUT"
echo "" >> "$OUT"
echo "## API: GET /api/paises/pe/despliegues" >> "$OUT"
cap jq -c '[.[] | {id, tipo, origenId, estado, versionCore, plugins: [.plugins[] | .nombre + " " + .version], creadoPor, confirmadoPor}]' <<<"$DESP"
echo "$CAP" >> "$OUT"

# --- 3. kubectl: la imagen, los pods y los resourceVersion de synkra-pe
echo "" >> "$OUT"
echo "## kubectl: synkra-pe" >> "$OUT"
cap kc -n synkra-pe get deploy -o jsonpath='{..image}'
echo "imagen: ${CAP:-no hay}" >> "$OUT"
cap kc -n synkra-pe get pods -o wide
echo "pods:" >> "$OUT"; echo "${CAP:-no hay pods}" >> "$OUT"
cap kc -n synkra-pe get deploy synkra-core --ignore-not-found -o jsonpath='{.metadata.resourceVersion}'
echo "resourceVersion Deployment/synkra-core" >> "$OUT"; echo "${CAP:-no hay}" >> "$OUT"
cap kc -n synkra-pe get configmap synkra-core-config --ignore-not-found -o jsonpath='{.metadata.resourceVersion}'
echo "resourceVersion ConfigMap/synkra-core-config" >> "$OUT"; echo "${CAP:-no hay}" >> "$OUT"

printf '%s\n' "$AHORA" > "$MARCA"
echo "ok paso $N $NOMBRE -> ${OUT#"$RAIZ"/}"
