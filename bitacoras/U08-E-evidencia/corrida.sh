#!/usr/bin/env bash
# U08-E, CA-1: la corrida fresca, en orden. Borra kind-synkra y lo instala de nuevo; escribe code/evidencia/{install,limite,e2e}.txt.
# Uso (desde la raiz del worktree): JAVA_HOME=<java 21> bash bitacoras/U08-E-evidencia/corrida.sh
set -u
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$RAIZ"
EV=aidlc-docs/construction/U08-instalacion-limite/code/evidencia
mkdir -p "$EV"
export KUBECONFIG_SYNKRA="$RAIZ/.kube/kind-synkra.yaml"
SHA="$(git rev-parse --short HEAD)"

cabecera() { # cabecera <archivo> <comando>
  {
    echo "fecha: $(date -Iseconds)"
    echo "sha: $SHA"
    echo "comando: $2"
    echo "docker-servidor-arquitectura: $(docker info --format '{{.Architecture}}')"
    echo "JAVA_HOME: ${JAVA_HOME:-sin definir}"
    echo "---"
  } >"$1"
}
paso() { # paso <archivo> <comando...>: la salida completa y, al final, salida=N y segundos=N
  local f="$1" t0=$SECONDS rc=0
  shift
  cabecera "$f" "$*"
  "$@" >>"$f" 2>&1 || rc=$?
  { echo "salida=$rc"; echo "segundos=$((SECONDS - t0))"; } >>"$f"
  return "$rc"
}

# Borrar antes de instalar: su salida va a un archivo de la bitacora (no es evidencia de CA-1, solo el borrado).
paso bitacoras/U08-E-evidencia/borrar.txt make cluster-delete || true
paso "$EV/install.txt" make install || { echo "install fallo"; exit 1; }
paso "$EV/limite.txt" make verificar-limite || { echo "limite fallo"; exit 1; }
paso "$EV/e2e.txt" ./scripts/e2e-api.sh || { echo "e2e fallo"; exit 1; }
echo "corrida terminada"
